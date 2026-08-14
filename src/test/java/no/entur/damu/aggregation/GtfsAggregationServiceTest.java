package no.entur.damu.aggregation;

import static no.entur.damu.Constants.BLOBSTORE_PATH_OUTBOUND;
import static no.entur.damu.Constants.CORRELATION_ID;
import static no.entur.damu.Constants.STATUS_HEADER;
import static no.entur.damu.aggregation.GtfsAggregationService.AGGREGATED_GTFS_BASIC_FILE_NAME;
import static no.entur.damu.aggregation.GtfsAggregationService.AGGREGATED_GTFS_EXTENDED_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import no.entur.damu.DamuPipelineTestBase;
import no.entur.damu.RecordingPubSubPublisher;
import no.entur.damu.pubsub.DamuQueues;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GtfsAggregationServiceTest extends DamuPipelineTestBase {

  private static final String TEST_CORRELATION_ID = "test-correlation-123";

  @Autowired
  private GtfsAggregationService gtfsAggregationService;

  @Test
  void mergesTheProviderExportsAndNotifiesMarduk() throws Exception {
    mardukInMemoryBlobStoreRepository.uploadBlob(
      BLOBSTORE_PATH_OUTBOUND + "gtfs/gtfs.zip",
      testResource("gtfs.zip")
    );
    mardukInMemoryBlobStoreRepository.uploadBlob(
      BLOBSTORE_PATH_OUTBOUND + "gtfs/gtfs2.zip",
      testResource("gtfs2.zip")
    );

    gtfsAggregationService.aggregate(
      "gtfs.zip,gtfs2.zip",
      Map.of(CORRELATION_ID, TEST_CORRELATION_ID)
    );

    assertNotEmptyBlob(
      mardukInMemoryBlobStoreRepository,
      BLOBSTORE_PATH_OUTBOUND + "gtfs/" + AGGREGATED_GTFS_EXTENDED_FILE_NAME
    );
    assertNotEmptyBlob(
      mardukInMemoryBlobStoreRepository,
      BLOBSTORE_PATH_OUTBOUND + "gtfs/" + AGGREGATED_GTFS_BASIC_FILE_NAME
    );

    assertEquals(List.of("started", "ok"), statuses());
    for (RecordingPubSubPublisher.Published status : publisher.publishedTo(
      DamuQueues.MARDUK_AGGREGATE_GTFS_STATUS_QUEUE
    )) {
      assertEquals(
        TEST_CORRELATION_ID,
        status.attributes().get(CORRELATION_ID)
      );
    }
  }

  @Test
  void notifiesMardukWhenTheMergeFails() {
    gtfsAggregationService.aggregate(
      "nosuchfile.zip",
      Map.of(CORRELATION_ID, TEST_CORRELATION_ID)
    );

    assertEquals(
      List.of("started", "failed"),
      statuses(),
      "A failed aggregation is terminal: marduk is told, and the message is acked rather than retried"
    );
  }

  private List<String> statuses() {
    return publisher
      .publishedTo(DamuQueues.MARDUK_AGGREGATE_GTFS_STATUS_QUEUE)
      .stream()
      .map(published -> published.attributes().get(STATUS_HEADER))
      .toList();
  }
}
