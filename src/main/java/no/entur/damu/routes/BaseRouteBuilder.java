/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *  *
 *
 */

package no.entur.damu.routes;

import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.pubsub.v1.ModifyAckDeadlineRequest;
import com.google.pubsub.v1.ProjectSubscriptionName;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.entur.damu.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.google.pubsub.GooglePubsubConstants;
import org.apache.camel.component.google.pubsub.GooglePubsubEndpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileSystemUtils;

/**
 * Defines common route behavior.
 */
public abstract class BaseRouteBuilder extends RouteBuilder {

  private static final String SYNCHRONIZATION_HOLDER = "SYNCHRONIZATION_HOLDER";

  @Value("${quartz.lenient.fire.time.ms:180000}")
  private int lenientFireTimeMs;

  @Value("${damu.camel.redelivery.max:3}")
  private int maxRedelivery;

  @Value("${damu.camel.redelivery.delay:5000}")
  private int redeliveryDelay;

  @Value("${damu.camel.redelivery.backoff.multiplier:3}")
  private int backOffMultiplier;

  @Value("${damu.camel.pubsub.deadline.extension:600}")
  private int deadlineExtension;

  @Override
  public void configure() throws Exception {
    errorHandler(
      defaultErrorHandler()
        .redeliveryDelay(redeliveryDelay)
        .maximumRedeliveries(maxRedelivery)
        .onRedelivery(this::logRedelivery)
        .useExponentialBackOff()
        .backOffMultiplier(backOffMultiplier)
        .logExhausted(true)
        .logRetryStackTrace(true)
    );

    // Copy all PubSub headers except the internal Camel PubSub headers from the PubSub message into the Camel message headers.

    interceptFrom("*")
      .filter(exchange ->
        exchange.getFromEndpoint() instanceof GooglePubsubEndpoint
      )
      .process(exchange -> {
        Map<String, String> pubSubAttributes = exchange
          .getIn()
          .getHeader(GooglePubsubConstants.ATTRIBUTES, Map.class);
        pubSubAttributes
          .entrySet()
          .stream()
          .filter(entry -> !entry.getKey().startsWith("CamelGooglePubsub"))
          .forEach(entry ->
            exchange.getIn().setHeader(entry.getKey(), entry.getValue())
          );
      });

    // Copy only the correlationId and codespace headers from the Camel message into the PubSub message by default.
    interceptSendToEndpoint("google-pubsub:*")
      .process(exchange -> {
        Map<String, String> pubSubAttributes = new HashMap<>(
          exchange
            .getIn()
            .getHeader(
              GooglePubsubConstants.ATTRIBUTES,
              new HashMap<>(),
              Map.class
            )
        );

        if (exchange.getIn().getHeader(Constants.CORRELATION_ID) != null) {
          pubSubAttributes.put(
            Constants.CORRELATION_ID,
            exchange.getIn().getHeader(Constants.CORRELATION_ID, String.class)
          );
        }
        if (exchange.getIn().getHeader(Constants.DATASET_REFERENTIAL) != null) {
          pubSubAttributes.put(
            Constants.DATASET_REFERENTIAL,
            exchange
              .getIn()
              .getHeader(Constants.DATASET_REFERENTIAL, String.class)
          );
        }

        if (exchange.getIn().getHeader(Constants.PROVIDER_ID) != null) {
          pubSubAttributes.put(
            Constants.PROVIDER_ID,
            exchange.getIn().getHeader(Constants.PROVIDER_ID, String.class)
          );
        }

        if (
          exchange.getIn().getHeader(Constants.ORIGINAL_PROVIDER_ID) != null
        ) {
          pubSubAttributes.put(
            Constants.ORIGINAL_PROVIDER_ID,
            exchange
              .getIn()
              .getHeader(Constants.ORIGINAL_PROVIDER_ID, String.class)
          );
        }

        exchange
          .getIn()
          .setHeader(GooglePubsubConstants.ATTRIBUTES, pubSubAttributes);
      });
  }

  protected void logRedelivery(Exchange exchange) {
    int redeliveryCounter = exchange
      .getIn()
      .getHeader("CamelRedeliveryCounter", Integer.class);
    int redeliveryMaxCounter = exchange
      .getIn()
      .getHeader("CamelRedeliveryMaxCounter", Integer.class);
    Throwable camelCaughtThrowable = exchange.getProperty(
      "CamelExceptionCaught",
      Throwable.class
    );
    String correlation = simple(correlation(), String.class)
      .evaluate(exchange, String.class);

    log.warn(
      "{} Exchange failed, redelivering the message locally, attempt {}/{}...",
      correlation,
      redeliveryCounter,
      redeliveryMaxCounter,
      camelCaughtThrowable
    );
  }

  protected String logDebugShowAll() {
    return (
      "log:" +
      getClass().getName() +
      "?level=DEBUG&showAll=true&multiline=true&showCachedStreams=false"
    );
  }

  protected void setNewCorrelationId(Exchange e) {
    e.getIn().setHeader(Constants.CORRELATION_ID, UUID.randomUUID().toString());
  }

  protected void setCorrelationIdIfMissing(Exchange e) {
    e
      .getIn()
      .setHeader(
        Constants.CORRELATION_ID,
        e
          .getIn()
          .getHeader(Constants.CORRELATION_ID, UUID.randomUUID().toString())
      );
  }

  protected String correlation() {
    return (
      "[codespace=${header." +
      Constants.DATASET_REFERENTIAL +
      "} correlationId=${header." +
      Constants.CORRELATION_ID +
      "}] "
    );
  }

  public void extendAckDeadline(Exchange exchange) throws IOException {
    String ackId = exchange
      .getIn()
      .getHeader(GooglePubsubConstants.ACK_ID, String.class);
    GooglePubsubEndpoint fromEndpoint =
      (GooglePubsubEndpoint) exchange.getFromEndpoint();
    String subscriptionName = ProjectSubscriptionName.format(
      fromEndpoint.getProjectId(),
      fromEndpoint.getDestinationName()
    );
    ModifyAckDeadlineRequest modifyAckDeadlineRequest = ModifyAckDeadlineRequest
      .newBuilder()
      .setSubscription(subscriptionName)
      .addAllAckIds(List.of(ackId))
      .setAckDeadlineSeconds(deadlineExtension)
      .build();
    try (
      SubscriberStub subscriberStub = fromEndpoint
        .getComponent()
        .getSubscriberStub(fromEndpoint)
    ) {
      subscriberStub.modifyAckDeadlineCallable().call(modifyAckDeadlineRequest);
    }
  }

  protected void deleteDirectoryRecursively(String directory) {
    log.debug("Deleting local directory {} ...", directory);
    try {
      Path pathToDelete = Paths.get(directory);
      boolean deleted = FileSystemUtils.deleteRecursively(pathToDelete);
      if (deleted) {
        log.debug("Local directory {} cleanup done.", directory);
      } else {
        log.debug("The directory {} did not exist, ignoring deletion request", directory);
      }
    } catch (IOException e) {
      log.warn("Failed to delete directory {}", directory, e);
    }
  }
}
