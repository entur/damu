package no.entur.damu.routes.validation;

import static no.entur.damu.Constants.DATASET_REFERENTIAL;
import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.util.Map;
import no.entur.damu.Constants;
import no.entur.damu.DamuRouteBuilderIntegrationTestBase;
import no.entur.damu.TestApp;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  classes = TestApp.class
)
class GtfsValidationQueueRouteBuilderTest
  extends DamuRouteBuilderIntegrationTestBase {

  public static final String CODESPACE = "rb_avi";

  // The outermost block of routes first:
  @Produce("direct:validateGtfs")
  private ProducerTemplate validateGtfsTemplate;

  @EndpointInject("mock:validationAndUploadCompleted")
  private MockEndpoint validationAndUploadCompleted;

  // We also test the inner routes individually:
  @Produce("direct:validateGtfsDataset")
  private ProducerTemplate validateGtfsDatasetTemplate;

  @Produce("direct:uploadValidationReports")
  private ProducerTemplate uploadValidationTemplate;

  @EndpointInject("mock:validationCompleted")
  private MockEndpoint validationCompleted;

  @EndpointInject("mock:reportUploadCompleted")
  private MockEndpoint reportUploadCompleted;

  @Test
  void testValidateAndUploadInOne() throws Exception {
    AdviceWith.adviceWith(
      context,
      "validate-gtfs",
      a -> {
        a.weaveAddLast().to("mock:validationAndUploadCompleted");
      }
    );

    validationAndUploadCompleted.expectedMessageCount(1);
    context.start();

    InputStream testGtfsStream = getClass()
      .getResourceAsStream("/rb_avi-aggregated-gtfs.zip");
    assertNotNull(testGtfsStream, "Test GTFS file not found on classpath");

    validateGtfsTemplate.sendBodyAndHeaders(
      testGtfsStream,
      Map.of(DATASET_REFERENTIAL, CODESPACE)
    );
    validationAndUploadCompleted.assertIsSatisfied();
  }

  @Test
  void testValidateGtfsAndUploadReports() throws Exception {
    AdviceWith.adviceWith(
      context,
      "validate-gtfs-dataset",
      a -> {
        a.weaveAddLast().to("mock:validationCompleted");
      }
    );

    AdviceWith.adviceWith(
      context,
      "upload-gtfs-validation-reports",
      a -> {
        a.weaveAddLast().to("mock:reportUploadCompleted");
      }
    );

    validationCompleted.expectedMessageCount(1);
    reportUploadCompleted.expectedMessageCount(1);
    context.start();

    InputStream testGtfsStream = getClass()
      .getResourceAsStream("/rb_avi-aggregated-gtfs.zip");
    assertNotNull(testGtfsStream, "Test GTFS file not found on classpath");

    validateGtfsDatasetTemplate.sendBodyAndHeaders(
      testGtfsStream,
      Map.of(DATASET_REFERENTIAL, CODESPACE)
    );
    InputStream validationReportStream = validationCompleted
      .getExchanges()
      .getFirst()
      .getIn()
      .getBody(InputStream.class);
    assertNotNull(validationReportStream, "Validation report must not be null");

    uploadValidationTemplate.sendBodyAndHeaders(
      validationReportStream,
      Map.of(DATASET_REFERENTIAL, CODESPACE)
    );
    reportUploadCompleted.assertIsSatisfied();
    validationCompleted.assertIsSatisfied();

    String expectedPath =
      Constants.GTFS_VALIDATION_REPORTS_FILENAME_PREFIX +
      CODESPACE +
      Constants.GTFS_VALIDATION_REPORTS_FILENAME_SUFFIX;
    InputStream uploadedReportStream = damuInMemoryBlobStoreRepository.getBlob(
      expectedPath
    );
    assertNotNull(
      uploadedReportStream,
      "Validation report should exist in the in-memory blob store at path: " +
      expectedPath
    );

    byte[] uploadedBytes = uploadedReportStream.readAllBytes();
    assertTrue(
      uploadedBytes.length > 0,
      "Uploaded validation report must be non-empty"
    );
  }
}
