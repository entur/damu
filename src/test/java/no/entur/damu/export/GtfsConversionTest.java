package no.entur.damu.export;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import no.entur.damu.DamuPipelineTestBase;
import no.entur.damu.stop.QuayFetcher;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Quays missing from the stop area repository are fetched from the stop place registry during the
 * conversion.
 */
class GtfsConversionTest extends DamuPipelineTestBase {

  @Autowired
  private GtfsExportService gtfsExportService;

  @Autowired
  private StopAreaRepositoryFactory stopAreaRepositoryFactory;

  @MockitoBean
  private QuayFetcher quayFetcher;

  @Test
  void missingMultipleQuaysAreFetched() throws Exception {
    stopAreaRepositoryFactory.refreshStopAreaRepository(
      testResource("Airports_latest.zip")
    );
    removeAndMockFetchQuayById("NSR:Quay:1184");
    removeAndMockFetchQuayById("NSR:Quay:1202");

    convert("rb_avi");

    verify(quayFetcher, times(2)).tryFetch(anyString());
  }

  private void convert(String codespace) throws Exception {
    Path gtfs = gtfsExportService.convertToGtfs(
      codespace,
      testResource("rb_avi-aggregated-netex.zip")
    );
    try {
      Assertions.assertTrue(Files.size(gtfs) > 0);
    } finally {
      Files.deleteIfExists(gtfs);
    }
  }

  private void removeAndMockFetchQuayById(String quayId) {
    stopAreaRepositoryFactory
      .getStopAreaRepository()
      .getAllQuays()
      .removeIf(quay -> quay.getId().equals(quayId));

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
