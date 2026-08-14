package no.entur.damu.pubsub;

import static no.entur.damu.Constants.GTFS_ROUTE_DISPATCHER_AGGREGATION_HEADER_VALUE;
import static no.entur.damu.Constants.GTFS_ROUTE_DISPATCHER_EXPORT_HEADER_VALUE;
import static no.entur.damu.Constants.GTFS_ROUTE_DISPATCHER_HEADER_NAME;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import no.entur.damu.aggregation.GtfsAggregationService;
import no.entur.damu.exception.DamuException;
import no.entur.damu.export.GtfsExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GtfsRouteDispatcherConsumerTest {

  private GtfsExportService exportService;
  private GtfsAggregationService aggregationService;
  private GtfsRouteDispatcherConsumer consumer;

  @BeforeEach
  void setUp() {
    exportService = mock(GtfsExportService.class);
    aggregationService = mock(GtfsAggregationService.class);
    consumer =
      new GtfsRouteDispatcherConsumer(exportService, aggregationService);
  }

  @Test
  void exportRequestGoesToTheExportService() {
    consumer.onMessage(
      "rb_flb".getBytes(StandardCharsets.UTF_8),
      Map.of(
        GTFS_ROUTE_DISPATCHER_HEADER_NAME,
        GTFS_ROUTE_DISPATCHER_EXPORT_HEADER_VALUE
      )
    );

    verify(exportService).export(eq("rb_flb"), any());
    verifyNoInteractions(aggregationService);
  }

  @Test
  void aggregationRequestGoesToTheAggregationService() {
    consumer.onMessage(
      "a.zip,b.zip".getBytes(StandardCharsets.UTF_8),
      Map.of(
        GTFS_ROUTE_DISPATCHER_HEADER_NAME,
        GTFS_ROUTE_DISPATCHER_AGGREGATION_HEADER_VALUE
      )
    );

    verify(aggregationService).aggregate(eq("a.zip,b.zip"), any());
    verifyNoInteractions(exportService);
  }

  @Test
  void unknownActionIsAcked() {
    consumer.onMessage(
      "rb_flb".getBytes(StandardCharsets.UTF_8),
      Map.of(GTFS_ROUTE_DISPATCHER_HEADER_NAME, "Something")
    );

    verifyNoInteractions(exportService, aggregationService);
  }

  /**
   * The last delivery is nacked before any work is done, so PubSub dead-letters the message instead of
   * damu paying for another full export first.
   */
  @Test
  void lastDeliveryAttemptIsNackedWithoutRunningTheJob() {
    assertThrows(
      DamuException.class,
      () ->
        consumer.onMessage(
          "rb_flb".getBytes(StandardCharsets.UTF_8),
          Map.of(
            GTFS_ROUTE_DISPATCHER_HEADER_NAME,
            GTFS_ROUTE_DISPATCHER_EXPORT_HEADER_VALUE,
            PubSubAttributes.DELIVERY_ATTEMPT,
            String.valueOf(GtfsRouteDispatcherConsumer.MAX_DELIVERY_ATTEMPTS)
          )
        )
    );

    verifyNoInteractions(exportService, aggregationService);
  }

  @Test
  void earlierDeliveryAttemptsRunTheJob() {
    consumer.onMessage(
      "rb_flb".getBytes(StandardCharsets.UTF_8),
      Map.of(
        GTFS_ROUTE_DISPATCHER_HEADER_NAME,
        GTFS_ROUTE_DISPATCHER_EXPORT_HEADER_VALUE,
        PubSubAttributes.DELIVERY_ATTEMPT,
        String.valueOf(GtfsRouteDispatcherConsumer.MAX_DELIVERY_ATTEMPTS - 1)
      )
    );

    verify(exportService).export(eq("rb_flb"), any());
  }
}
