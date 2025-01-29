package no.entur.damu.routes.export;

import static no.entur.damu.Constants.DATASET_REFERENTIAL;
import static no.entur.damu.routes.export.GtfsExportQueueRouteBuilder.TIMETABLE_DATASET_FILE;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import no.entur.damu.DamuRouteBuilderIntegrationTestBase;
import no.entur.damu.TestApp;
import no.entur.damu.stop.QuayFetcher;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  classes = TestApp.class
)
class GtfsExportQueueRouteBuilderTest
  extends DamuRouteBuilderIntegrationTestBase {

  @Produce("direct:convertToGtfs")
  protected ProducerTemplate convertToGtfs;

  @EndpointInject("mock:gtfsDataset")
  private MockEndpoint gtfsDataset;

  @Autowired
  private StopAreaRepositoryFactory stopAreaRepositoryFactory;

  @MockBean
  private QuayFetcher quayFetcher;

  @Test
  void testMissingSingleQuayIsFetched() throws Exception {
    // Adding the mock endpoint in order to verify the route ends properly.
    AdviceWith.adviceWith(
      context,
      "convert-to-gtfs",
      a -> a.weaveAddLast().to("mock:gtfsDataset")
    );

    // Populating the stopAreaRepository with stop areas.
    stopAreaRepositoryFactory.refreshStopAreaRepository(
      getClass().getResourceAsStream("/Airports_latest.zip")
    );

    removeAndMockFetchQuayById("NSR:Quay:1184");

    context.start();

    gtfsDataset.expectedMessageCount(1);

    // Starting the camel route by sending the aggregated netex file.
    convertToGtfs.sendBodyAndHeaders(
      null,
      Map.of(
        TIMETABLE_DATASET_FILE,
        Objects.requireNonNull(
          getClass().getResourceAsStream("/rb_avi-aggregated-netex.zip")
        ),
        DATASET_REFERENTIAL,
        "rb_avi"
      )
    );

    // Verifying the route end properly.
    gtfsDataset.assertIsSatisfied();

    // verifying the tryFetchQuay called 1 time, for the missing quay.
    verify(quayFetcher, times(1)).tryFetch(anyString());
  }

  @Test
  void testMissingMultipleQuaysAreFetched() throws Exception {
    // Adding the mock endpoint in order to verify the route ends properly.
    AdviceWith.adviceWith(
      context,
      "convert-to-gtfs",
      a -> a.weaveAddLast().to("mock:gtfsDataset")
    );

    // Populating the stopAreaRepository with stop areas.
    stopAreaRepositoryFactory.refreshStopAreaRepository(
      getClass().getResourceAsStream("/Airports_latest.zip")
    );

    removeAndMockFetchQuayById("NSR:Quay:1184");
    removeAndMockFetchQuayById("NSR:Quay:1202");

    context.start();

    gtfsDataset.expectedMessageCount(1);

    // Starting the camel route by sending the aggregated netex file.
    convertToGtfs.sendBodyAndHeaders(
      null,
      Map.of(
        TIMETABLE_DATASET_FILE,
        Objects.requireNonNull(
          getClass().getResourceAsStream("/rb_avi-aggregated-netex.zip")
        ),
        DATASET_REFERENTIAL,
        "rb_flb"
      )
    );

    // Verifying the route end properly.
    gtfsDataset.assertIsSatisfied();

    // verifying the tryFetchQuay called 1 time, for the missing quay.
    verify(quayFetcher, times(2)).tryFetch(anyString());
  }

  private void removeAndMockFetchQuayById(String quayId) {
    // Removing the quay with the given id from the repository,
    // to simulate the situation where the quay not exists.
    stopAreaRepositoryFactory
      .getStopAreaRepository()
      .getAllQuays()
      .stream()
      .filter(quay -> quay.getId().equals(quayId))
      .findFirst()
      .ifPresent(quay ->
        stopAreaRepositoryFactory
          .getStopAreaRepository()
          .getAllQuays()
          .remove(quay)
      );

    // Mocking the tryFetchQuay.
    when(quayFetcher.tryFetch(quayId))
      .thenReturn(
        new Quay()
          .withId(quayId)
          .withCentroid(
            new SimplePoint_VersionStructure()
              .withLocation(
                new LocationStructure()
                  .withLatitude(BigDecimal.valueOf(9.614056))
                  .withLongitude(BigDecimal.valueOf(63.701134))
              )
          )
      );
  }
}
