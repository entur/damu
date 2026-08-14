package no.entur.damu.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import no.entur.damu.exception.DamuException;
import org.springframework.stereotype.Component;

/**
 * Publishes through Spring Cloud GCP and blocks until the broker has confirmed.
 *
 * <p>Every publish tells marduk what happened to a job, so a silent failure would leave the job
 * without a terminal status.
 */
@Component
class PubSubTemplatePublisher implements PubSubPublisher {

  private final PubSubTemplate pubSubTemplate;

  PubSubTemplatePublisher(PubSubTemplate pubSubTemplate) {
    this.pubSubTemplate = pubSubTemplate;
  }

  @Override
  public void publish(
    String destination,
    String body,
    Map<String, String> attributes
  ) {
    try {
      pubSubTemplate.publish(destination, body, attributes).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new DamuException(
        "Interrupted while publishing to " + destination,
        e
      );
    } catch (ExecutionException e) {
      throw new DamuException("Failed to publish to " + destination, e);
    }
  }
}
