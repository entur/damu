package no.entur.damu.pubsub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.entur.damu.Constants;
import no.entur.damu.aggregation.GtfsAggregationService;
import no.entur.damu.export.GtfsStopExportService;
import org.junit.jupiter.api.Test;

/**
 * Everything about damu that something outside this repository matches by name.
 *
 * <p>Asserted against hard-coded strings on purpose. Comparing a value to the constant that holds it
 * pins nothing: renaming the value keeps both sides in step and the test green, while marduk, the
 * terraformed topics and the published blob paths all move out from under it.
 */
class WireContractTest {

  @Test
  void pubSubDestinations() {
    assertEquals(
      "GtfsRouteDispatcherTopic",
      DamuQueues.GTFS_ROUTE_DISPATCHER_TOPIC
    );
    assertEquals(
      "DamuExportGtfsStatusQueue",
      DamuQueues.DAMU_EXPORT_GTFS_STATUS_QUEUE
    );
    assertEquals(
      "MardukAggregateGtfsStatusQueue",
      DamuQueues.MARDUK_AGGREGATE_GTFS_STATUS_QUEUE
    );
  }

  /**
   * Must stay equal to {@code dead_letter_policy.max_delivery_attempts} on the GtfsRouteDispatcherTopic
   * subscription in marduk's terraform/pubsub.tf.
   */
  @Test
  void maxDeliveryAttempts() {
    assertEquals(5, GtfsRouteDispatcherConsumer.MAX_DELIVERY_ATTEMPTS);
  }

  @Test
  void requestAttributes() {
    assertEquals("Action", Constants.GTFS_ROUTE_DISPATCHER_HEADER_NAME);
    assertEquals("Export", Constants.GTFS_ROUTE_DISPATCHER_EXPORT_HEADER_VALUE);
    assertEquals(
      "Aggregation",
      Constants.GTFS_ROUTE_DISPATCHER_AGGREGATION_HEADER_VALUE
    );
  }

  @Test
  void attributesEchoedBackToMarduk() {
    assertEquals("RutebankenCorrelationId", Constants.CORRELATION_ID);
    assertEquals("RutebankenProviderId", Constants.PROVIDER_ID);
    assertEquals(
      "RutebankenOriginalProviderId",
      Constants.ORIGINAL_PROVIDER_ID
    );
    assertEquals("EnturDatasetReferential", Constants.DATASET_REFERENTIAL);
  }

  /**
   * The export status travels in the message body, the aggregation status in an attribute. Marduk
   * matches on both.
   */
  @Test
  void statusValues() {
    assertEquals("started", Constants.STATUS_EXPORT_STARTED);
    assertEquals("ok", Constants.STATUS_EXPORT_OK);
    assertEquals("failed", Constants.STATUS_EXPORT_FAILED);

    assertEquals("status", Constants.STATUS_HEADER);
    assertEquals("started", Constants.STATUS_MERGE_STARTED);
    assertEquals("ok", Constants.STATUS_MERGE_OK);
    assertEquals("failed", Constants.STATUS_MERGE_FAILED);
  }

  @Test
  void blobStorePaths() {
    assertEquals("outbound/", Constants.BLOBSTORE_PATH_OUTBOUND);
    assertEquals("netex/", Constants.NETEX_FILENAME_PREFIX);
    assertEquals("-aggregated-netex.zip", Constants.NETEX_FILENAME_SUFFIX);
    assertEquals("gtfs/", Constants.GTFS_FILENAME_PREFIX);
    assertEquals("-aggregated-gtfs.zip", Constants.GTFS_FILENAME_SUFFIX);
    assertEquals(
      "gtfsreport.entur.org/",
      Constants.GTFS_VALIDATION_REPORTS_FILENAME_PREFIX
    );
    assertEquals(
      "-gtfs-validation-reports.zip",
      Constants.GTFS_VALIDATION_REPORTS_FILENAME_SUFFIX
    );
    assertEquals(
      "tiamat/Current_latest-gtfs.zip",
      GtfsStopExportService.GTFS_STOP_EXPORT_FILE_NAME
    );
  }

  /**
   * Read by everything downstream of the national GTFS export.
   */
  @Test
  void aggregatedDatasetNames() {
    assertEquals(
      "rb_norway-aggregated-gtfs.zip",
      GtfsAggregationService.AGGREGATED_GTFS_EXTENDED_FILE_NAME
    );
    assertEquals(
      "rb_norway-aggregated-gtfs-basic.zip",
      GtfsAggregationService.AGGREGATED_GTFS_BASIC_FILE_NAME
    );
  }

  /**
   * The prefix PubSub reserves. It is what the delivery attempt counter arrives under, and republishing
   * anything carrying it is rejected on publish. Same value as Camel's
   * {@code GooglePubsubConstants.RESERVED_GOOGLE_CLIENT_ATTRIBUTE_PREFIX}.
   */
  @Test
  void reservedAttributePrefix() {
    assertEquals("goog", PubSubAttributes.RESERVED_GOOGLE_CLIENT_PREFIX);
    assertEquals(
      "googclient_deliveryattempt",
      PubSubAttributes.DELIVERY_ATTEMPT
    );
  }
}
