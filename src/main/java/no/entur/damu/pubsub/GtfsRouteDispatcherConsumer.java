package no.entur.damu.pubsub;

import static no.entur.damu.Constants.CORRELATION_ID;
import static no.entur.damu.Constants.DATASET_REFERENTIAL;
import static no.entur.damu.Constants.GTFS_ROUTE_DISPATCHER_AGGREGATION_HEADER_VALUE;
import static no.entur.damu.Constants.GTFS_ROUTE_DISPATCHER_EXPORT_HEADER_VALUE;
import static no.entur.damu.Constants.GTFS_ROUTE_DISPATCHER_HEADER_NAME;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import no.entur.damu.DamuMdc;
import no.entur.damu.aggregation.GtfsAggregationService;
import no.entur.damu.exception.DamuException;
import no.entur.damu.export.GtfsExportService;
import org.entur.pubsub.base.AbstractEnturGooglePubSubConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumes marduk's work requests and runs the matching job.
 *
 * <p>The subscription is served by the PubSub streaming pull client, which extends the ack deadline on
 * its own while a message is being processed, for up to an hour. A GTFS export or aggregation takes
 * minutes, and this is what keeps it from being redelivered mid-run.
 */
@Component
@ConditionalOnProperty(
  value = "damu.pubsub.consumers.enabled",
  matchIfMissing = true
)
public class GtfsRouteDispatcherConsumer
  extends AbstractEnturGooglePubSubConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    GtfsRouteDispatcherConsumer.class
  );

  /**
   * Must match {@code dead_letter_policy.max_delivery_attempts} on the GtfsRouteDispatcherTopic
   * subscription in marduk's terraform/pubsub.tf. On the last attempt the message is nacked without
   * being processed, so PubSub dead-letters it instead of damu paying for another full export first.
   */
  static final int MAX_DELIVERY_ATTEMPTS = 5;

  private final GtfsExportService gtfsExportService;
  private final GtfsAggregationService gtfsAggregationService;

  public GtfsRouteDispatcherConsumer(
    GtfsExportService gtfsExportService,
    GtfsAggregationService gtfsAggregationService
  ) {
    this.gtfsExportService = gtfsExportService;
    this.gtfsAggregationService = gtfsAggregationService;
  }

  @Override
  protected String getDestinationName() {
    return DamuQueues.GTFS_ROUTE_DISPATCHER_TOPIC;
  }

  @Override
  public void onMessage(byte[] content, Map<String, String> attributes) {
    // Cleared on the way in, not on the way out: the consumer base class logs a failed message after
    // onMessage returns, and that ERROR line is the one worth having the job's identity on.
    DamuMdc.clear();
    DamuMdc.setCorrelationId(attributes.get(CORRELATION_ID));
    DamuMdc.setCodespace(attributes.get(DATASET_REFERENTIAL));

    int deliveryAttempt = PubSubAttributes.deliveryAttempt(attributes);
    if (deliveryAttempt >= MAX_DELIVERY_ATTEMPTS) {
      throw new DamuException(
        "Reached max delivery attempts (" +
        deliveryAttempt +
        "/" +
        MAX_DELIVERY_ATTEMPTS +
        "), nacking to route the message to the dead letter topic"
      );
    }

    String body = new String(content, StandardCharsets.UTF_8);
    String action = attributes.get(GTFS_ROUTE_DISPATCHER_HEADER_NAME);
    if (GTFS_ROUTE_DISPATCHER_EXPORT_HEADER_VALUE.equals(action)) {
      LOGGER.info("Dispatching message to GTFS export");
      gtfsExportService.export(body, attributes);
    } else if (GTFS_ROUTE_DISPATCHER_AGGREGATION_HEADER_VALUE.equals(action)) {
      LOGGER.info("Dispatching message to GTFS aggregation");
      gtfsAggregationService.aggregate(body, attributes);
    } else {
      LOGGER.info("Unknown header value {}, ignoring message", action);
    }
  }
}
