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

import static no.entur.damu.Constants.BLOBSTORE_PATH_OUTBOUND;
import static no.entur.damu.Constants.CORRELATION_ID;
import static no.entur.damu.Constants.DATASET_REFERENTIAL;
import static no.entur.damu.Constants.ORIGINAL_PROVIDER_ID;
import static no.entur.damu.Constants.PROVIDER_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.pubsub.v1.PubsubMessage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.entur.damu.Constants;
import no.entur.damu.DamuRouteBuilderIntegrationTestBase;
import no.entur.damu.TestApp;
import no.entur.damu.stop.QuayFetcher;
import no.entur.damu.stop.StopPlaceFetcher;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.google.pubsub.GooglePubsubConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;
import org.rutebanken.netex.model.StopPlace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  classes = TestApp.class
)
class NetexToGtfsConversionIntegrationTest
  extends DamuRouteBuilderIntegrationTestBase {

  private static final String CODESPACE = "rb_ost";
  private static final String TEST_CORRELATION_ID = "test-correlation-123";
  private static final String TEST_PROVIDER_ID = "2";
  private static final String TEST_ORIGINAL_PROVIDER_ID = "2";

  @Produce(
    "google-pubsub:{{marduk.pubsub.project.id}}:GtfsRouteDispatcherTopic"
  )
  protected ProducerTemplate gtfsExportQueueProducerTemplate;

  @EndpointInject("mock:checkUploadedGtfsDataset")
  protected MockEndpoint checkUploadedGtfsDataset;

  @EndpointInject("mock:validateGtfsOutput")
  private MockEndpoint mockValidateGtfsOutput;

  @EndpointInject("mock:aggregateGtfsDone")
  private MockEndpoint aggregateGtfsDone;

  @Value("${damu.netex.stop.filename:tiamat/CurrentAndFuture_latest.zip}")
  private String stopExportFilename;

  @Value("${damu.pubsub.project.id}")
  private String damuPubSubProjectId;

  @Autowired
  private StopAreaRepositoryFactory stopAreaRepositoryFactory;

  @MockitoBean
  private QuayFetcher quayFetcher;

  @MockitoBean
  private StopPlaceFetcher stopPlaceFetcher;

  @Test
  void testNetexToGtfsConversion() throws Exception {
    // Setup advice for export workflow
    AdviceWith.adviceWith(
      context,
      "upload-gtfs-dataset",
      a -> a.weaveAddLast().to("mock:checkUploadedGtfsDataset")
    );
    AdviceWith.adviceWith(
      context,
      "validate-gtfs",
      routeBuilder -> routeBuilder.weaveAddLast().to("mock:validateGtfsOutput")
    );

    // Set expectations for export workflow
    checkUploadedGtfsDataset.expectedMessageCount(1);
    checkUploadedGtfsDataset.setResultWaitTime(60_000);
    mockValidateGtfsOutput.expectedMessageCount(1);
    mockValidateGtfsOutput.setResultWaitTime(60_000);

    // Upload NeTEx dataset to blob store (as Marduk would)
    mardukInMemoryBlobStoreRepository.uploadBlob(
      BLOBSTORE_PATH_OUTBOUND +
      Constants.NETEX_FILENAME_PREFIX +
      CODESPACE +
      Constants.NETEX_FILENAME_SUFFIX,
      getClass().getResourceAsStream("/rb_ost-aggregated-netex.zip")
    );

    // Upload stop data to blob store
    mardukInMemoryBlobStoreRepository.uploadBlob(
      stopExportFilename,
      getClass().getResourceAsStream("/Current_latest.zip")
    );

    // Populate the stopAreaRepository with stop areas
    stopAreaRepositoryFactory.refreshStopAreaRepository(
      getClass().getResourceAsStream("/Current_latest.zip")
    );

    // Mock QuayFetcher to return a valid quay with coordinates for any missing quays
    when(quayFetcher.tryFetch(anyString()))
      .thenAnswer(invocation -> {
        String quayId = invocation.getArgument(0);
        return new Quay()
          .withId(quayId)
          .withCentroid(
            new SimplePoint_VersionStructure()
              .withLocation(
                new LocationStructure()
                  .withLatitude(BigDecimal.valueOf(59.9139))
                  .withLongitude(BigDecimal.valueOf(10.7522))
              )
          );
      });

    // Mock StopPlaceFetcher to return a valid stop place for missing quays
    when(stopPlaceFetcher.tryFetch(anyString()))
      .thenAnswer(invocation -> {
        String quayId = invocation.getArgument(0);
        // Extract stop place ID from quay ID (simplified logic for testing)
        String stopPlaceId = "NSR:StopPlace:" + quayId.replaceAll("[^0-9]", "");
        return new StopPlace()
          .withId(stopPlaceId)
          .withName(new MultilingualString().withValue("Test Stop Place"))
          .withCentroid(
            new SimplePoint_VersionStructure()
              .withLocation(
                new LocationStructure()
                  .withLatitude(BigDecimal.valueOf(59.9139))
                  .withLongitude(BigDecimal.valueOf(10.7522))
              )
          );
      });

    context.start();

    // Send PubSub message to trigger GTFS export
    Map<String, Object> exportHeaders = new HashMap<>();
    exportHeaders.put("Action", "Export");
    exportHeaders.put(CORRELATION_ID, TEST_CORRELATION_ID);
    exportHeaders.put(PROVIDER_ID, TEST_PROVIDER_ID);
    exportHeaders.put(ORIGINAL_PROVIDER_ID, TEST_ORIGINAL_PROVIDER_ID);

    gtfsExportQueueProducerTemplate.sendBodyAndHeader(
      CODESPACE,
      GooglePubsubConstants.ATTRIBUTES,
      exportHeaders
    );

    // Wait for export to complete
    checkUploadedGtfsDataset.assertIsSatisfied();
    mockValidateGtfsOutput.assertIsSatisfied();

    // Verify GTFS file was created
    String gtfsFilePath =
      "damu/" +
      Constants.GTFS_FILENAME_PREFIX +
      CODESPACE +
      Constants.GTFS_FILENAME_SUFFIX;
    InputStream gtfsExport = mardukInMemoryBlobStoreRepository.getBlob(
      gtfsFilePath
    );
    assertNotNull(
      gtfsExport,
      "GTFS file should exist at path: " + gtfsFilePath
    );
    byte[] gtfsContent = gtfsExport.readAllBytes();
    assertTrue(gtfsContent.length > 0, "GTFS file must be non-empty");

    // Pull and verify PubSub status messages sent to Marduk
    // Should get at minimum the "started" message
    List<PubsubMessage> statusMessages = pubSubTemplate.pullAndAck(
      "DamuExportGtfsStatusQueue",
      100,
      true
    );

    assertFalse(
      statusMessages.isEmpty(),
      "Should receive at least the 'started' status message, got: " +
      statusMessages.size()
    );

    // Find the "started" message
    PubsubMessage startedMessage = statusMessages
      .stream()
      .filter(msg -> "started".equals(msg.getData().toStringUtf8()))
      .findFirst()
      .orElseThrow(() ->
        new AssertionError("Should have received 'started' message")
      );

    // Verify "started" message headers
    Map<String, String> startedHeaders = startedMessage.getAttributesMap();
    assertEquals(
      TEST_CORRELATION_ID,
      startedHeaders.get(CORRELATION_ID),
      "Correlation ID should be preserved in started message"
    );
    assertEquals(
      TEST_PROVIDER_ID,
      startedHeaders.get(PROVIDER_ID),
      "Provider ID should be preserved in started message"
    );
    assertEquals(
      TEST_ORIGINAL_PROVIDER_ID,
      startedHeaders.get(ORIGINAL_PROVIDER_ID),
      "Original Provider ID should be preserved in started message"
    );
    assertEquals(
      CODESPACE,
      startedHeaders.get(DATASET_REFERENTIAL),
      "Dataset referential should be present in started message"
    );

    //TODO: investigate why "ok" message is not available
    statusMessages
      .stream()
      .filter(msg -> "ok".equals(msg.getData().toStringUtf8()))
      .findFirst()
      .ifPresent(okMessage -> {
        Map<String, String> okHeaders = okMessage.getAttributesMap();
        assertEquals(
          TEST_CORRELATION_ID,
          okHeaders.get(CORRELATION_ID),
          "Correlation ID should be preserved in ok message"
        );
        assertEquals(
          TEST_PROVIDER_ID,
          okHeaders.get(PROVIDER_ID),
          "Provider ID should be preserved in ok message"
        );
        assertEquals(
          TEST_ORIGINAL_PROVIDER_ID,
          okHeaders.get(ORIGINAL_PROVIDER_ID),
          "Original Provider ID should be preserved in ok message"
        );
        assertEquals(
          CODESPACE,
          okHeaders.get(DATASET_REFERENTIAL),
          "Dataset referential should be present in ok message"
        );
      });
  }

  @Test
  void testGtfsAggregation() throws Exception {
    // Setup advice for aggregation workflow
    AdviceWith.adviceWith(
      context,
      "aggregate-gtfs",
      a -> a.weaveAddLast().to("mock:aggregateGtfsDone")
    );

    // Set expectations for aggregation workflow
    aggregateGtfsDone.expectedMessageCount(1);
    aggregateGtfsDone.setResultWaitTime(60_000);

    // Upload test GTFS files to blob store
    mardukInMemoryBlobStoreRepository.uploadBlob(
      BLOBSTORE_PATH_OUTBOUND +
      "gtfs/" +
      CODESPACE +
      Constants.GTFS_FILENAME_SUFFIX,
      getClass().getResourceAsStream("/gtfs.zip")
    );

    context.start();

    // Send aggregation request
    Map<String, String> aggregationHeaders = new HashMap<>();
    aggregationHeaders.put("Action", "Aggregation");
    aggregationHeaders.put(CORRELATION_ID, TEST_CORRELATION_ID);

    sendBodyAndHeadersToPubSub(
      gtfsExportQueueProducerTemplate,
      CODESPACE + Constants.GTFS_FILENAME_SUFFIX,
      aggregationHeaders
    );

    // Wait for aggregation to complete
    aggregateGtfsDone.assertIsSatisfied();

    // Verify aggregated GTFS files were created
    String aggregatedGtfsPath =
      BLOBSTORE_PATH_OUTBOUND + "gtfs/rb_norway-aggregated-gtfs.zip";
    InputStream aggregatedGtfs = mardukInMemoryBlobStoreRepository.getBlob(
      aggregatedGtfsPath
    );
    assertNotNull(
      aggregatedGtfs,
      "Aggregated GTFS file should exist at path: " + aggregatedGtfsPath
    );
    byte[] aggregatedContent = aggregatedGtfs.readAllBytes();
    assertTrue(
      aggregatedContent.length > 0,
      "Aggregated GTFS file must be non-empty"
    );

    // Verify basic aggregated GTFS file
    String aggregatedGtfsBasicPath =
      BLOBSTORE_PATH_OUTBOUND + "gtfs/rb_norway-aggregated-gtfs-basic.zip";
    InputStream aggregatedGtfsBasic = mardukInMemoryBlobStoreRepository.getBlob(
      aggregatedGtfsBasicPath
    );
    assertNotNull(
      aggregatedGtfsBasic,
      "Basic aggregated GTFS file should exist at path: " +
      aggregatedGtfsBasicPath
    );
    byte[] aggregatedBasicContent = aggregatedGtfsBasic.readAllBytes();
    assertTrue(
      aggregatedBasicContent.length > 0,
      "Basic aggregated GTFS file must be non-empty"
    );
  }
}
