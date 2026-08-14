package no.entur.damu.export;

import static no.entur.damu.Constants.BLOBSTORE_PATH_OUTBOUND;
import static no.entur.damu.Constants.CORRELATION_ID;
import static no.entur.damu.Constants.DATASET_REFERENTIAL;
import static no.entur.damu.Constants.GTFS_FILENAME_PREFIX;
import static no.entur.damu.Constants.GTFS_FILENAME_SUFFIX;
import static no.entur.damu.Constants.GTFS_VALIDATION_REPORTS_FILENAME_PREFIX;
import static no.entur.damu.Constants.GTFS_VALIDATION_REPORTS_FILENAME_SUFFIX;
import static no.entur.damu.Constants.NETEX_FILENAME_PREFIX;
import static no.entur.damu.Constants.NETEX_FILENAME_SUFFIX;
import static no.entur.damu.Constants.ORIGINAL_PROVIDER_ID;
import static no.entur.damu.Constants.PROVIDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import no.entur.damu.DamuPipelineTestBase;
import no.entur.damu.RecordingPubSubPublisher;
import no.entur.damu.pubsub.DamuQueues;
import no.entur.damu.stop.QuayFetcher;
import no.entur.damu.stop.StopPlaceFetcher;
import no.entur.damu.validation.GtfsValidationService;
import org.entur.netex.gtfs.export.exception.GtfsExportException;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;
import org.rutebanken.netex.model.StopPlace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class GtfsExportServiceTest extends DamuPipelineTestBase {

  private static final String CODESPACE = "rb_ost";
  private static final String TEST_CORRELATION_ID = "test-correlation-123";
  private static final String TEST_PROVIDER_ID = "2";

  @Autowired
  private GtfsExportService gtfsExportService;

  @Autowired
  private StopAreaRepositoryFactory stopAreaRepositoryFactory;

  @MockitoBean
  private QuayFetcher quayFetcher;

  @MockitoBean
  private StopPlaceFetcher stopPlaceFetcher;

  @MockitoSpyBean
  private GtfsValidationService gtfsValidationService;

  @Test
  void exportsValidatesAndNotifiesMarduk() throws Exception {
    mardukInMemoryBlobStoreRepository.uploadBlob(
      BLOBSTORE_PATH_OUTBOUND +
      NETEX_FILENAME_PREFIX +
      CODESPACE +
      NETEX_FILENAME_SUFFIX,
      testResource("rb_ost-aggregated-netex.zip")
    );
    stopAreaRepositoryFactory.refreshStopAreaRepository(
      testResource("Current_latest.zip")
    );
    mockMissingStopPlaceLookups();

    gtfsExportService.export(
      CODESPACE,
      Map.of(
        CORRELATION_ID,
        TEST_CORRELATION_ID,
        PROVIDER_ID,
        TEST_PROVIDER_ID,
        ORIGINAL_PROVIDER_ID,
        TEST_PROVIDER_ID
      )
    );

    assertNotEmptyBlob(
      mardukInMemoryBlobStoreRepository,
      "damu/" + GTFS_FILENAME_PREFIX + CODESPACE + GTFS_FILENAME_SUFFIX
    );
    assertNotEmptyBlob(
      damuInMemoryBlobStoreRepository,
      GTFS_VALIDATION_REPORTS_FILENAME_PREFIX +
      CODESPACE +
      GTFS_VALIDATION_REPORTS_FILENAME_SUFFIX
    );

    List<RecordingPubSubPublisher.Published> statuses = publisher.publishedTo(
      DamuQueues.DAMU_EXPORT_GTFS_STATUS_QUEUE
    );
    assertEquals(
      List.of("started", "ok"),
      statuses.stream().map(RecordingPubSubPublisher.Published::body).toList()
    );
    for (RecordingPubSubPublisher.Published status : statuses) {
      Map<String, String> attributes = status.attributes();
      assertEquals(TEST_CORRELATION_ID, attributes.get(CORRELATION_ID));
      assertEquals(TEST_PROVIDER_ID, attributes.get(PROVIDER_ID));
      assertEquals(TEST_PROVIDER_ID, attributes.get(ORIGINAL_PROVIDER_ID));
      assertEquals(CODESPACE, attributes.get(DATASET_REFERENTIAL));
      assertEquals(
        4,
        attributes.size(),
        "Only the request attributes and the referential belong on the status: " +
        attributes
      );
      attributes.forEach((key, value) ->
        assertTrue(
          value.getBytes(StandardCharsets.UTF_8).length <= 1024,
          "The value of attribute " + key + " must not exceed 1024 bytes"
        )
      );
    }
  }

  /**
   * Camel ran validation and upload as a sequential multicast with {@code stopOnException} at its
   * default, so a validation failure did not stop the upload. Reverting
   * {@link GtfsExportService#validateAndUpload} to two plain sequential calls fails this test.
   */
  @Test
  void validationFailureStillUploadsTheDatasetAndReportsFailed()
    throws Exception {
    mardukInMemoryBlobStoreRepository.uploadBlob(
      BLOBSTORE_PATH_OUTBOUND +
      NETEX_FILENAME_PREFIX +
      "rb_avi" +
      NETEX_FILENAME_SUFFIX,
      testResource("rb_avi-aggregated-netex.zip")
    );
    stopAreaRepositoryFactory.refreshStopAreaRepository(
      testResource("Airports_latest.zip")
    );
    mockMissingStopPlaceLookups();
    doThrow(new GtfsExportException("validation blew up"))
      .when(gtfsValidationService)
      .validateAndUploadReports(anyString(), any());

    gtfsExportService.export(
      "rb_avi",
      Map.of(CORRELATION_ID, TEST_CORRELATION_ID)
    );

    assertNotEmptyBlob(
      mardukInMemoryBlobStoreRepository,
      "damu/" + GTFS_FILENAME_PREFIX + "rb_avi" + GTFS_FILENAME_SUFFIX
    );
    assertEquals(
      List.of("started", "failed"),
      publisher
        .publishedTo(DamuQueues.DAMU_EXPORT_GTFS_STATUS_QUEUE)
        .stream()
        .map(RecordingPubSubPublisher.Published::body)
        .toList()
    );
  }

  @Test
  void missingNetexDatasetLeavesTheExportWithoutATerminalStatus() {
    gtfsExportService.export(
      "rb_nosuchcodespace",
      Map.of(CORRELATION_ID, TEST_CORRELATION_ID)
    );

    assertEquals(
      List.of("started"),
      publisher
        .publishedTo(DamuQueues.DAMU_EXPORT_GTFS_STATUS_QUEUE)
        .stream()
        .map(RecordingPubSubPublisher.Published::body)
        .toList(),
      "Camel stopped the route here, so marduk gets no ok and no failed. Change this only " +
      "together with the job state machine in marduk."
    );
  }

  @Test
  void generatesACorrelationIdWhenTheRequestHasNone() {
    gtfsExportService.export("rb_nosuchcodespace", Map.of());

    Map<String, String> attributes = publisher
      .publishedTo(DamuQueues.DAMU_EXPORT_GTFS_STATUS_QUEUE)
      .getFirst()
      .attributes();
    assertNotNull(attributes.get(CORRELATION_ID));
    assertFalse(attributes.get(CORRELATION_ID).isEmpty());
  }

  private void mockMissingStopPlaceLookups() {
    when(quayFetcher.tryFetch(anyString()))
      .thenAnswer(invocation ->
        new Quay()
          .withId(invocation.getArgument(0, String.class))
          .withCentroid(osloCentroid())
      );
    when(stopPlaceFetcher.tryFetch(anyString()))
      .thenAnswer(invocation ->
        new StopPlace()
          .withId(
            "NSR:StopPlace:" +
            invocation.getArgument(0, String.class).replaceAll("[^0-9]", "")
          )
          .withName(new MultilingualString().withValue("Test Stop Place"))
          .withCentroid(osloCentroid())
      );
  }

  private static SimplePoint_VersionStructure osloCentroid() {
    return new SimplePoint_VersionStructure()
      .withLocation(
        new LocationStructure()
          .withLatitude(BigDecimal.valueOf(59.9139))
          .withLongitude(BigDecimal.valueOf(10.7522))
      );
  }
}
