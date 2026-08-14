package no.entur.damu.export;

import static no.entur.damu.export.GtfsStopExportService.GTFS_STOP_EXPORT_FILE_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import no.entur.damu.DamuPipelineTestBase;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

class GtfsStopExportServiceTest extends DamuPipelineTestBase {

  @Autowired
  private GtfsStopExportService gtfsStopExportService;

  @Autowired
  private StopAreaRepositoryFactory stopAreaRepositoryFactory;

  @Value("${damu.netex.stop.current.filename:tiamat/Current_latest.zip}")
  private String stopExportFilename;

  @Test
  void exportsTheStopPlacesToGtfs() throws Exception {
    mardukInMemoryBlobStoreRepository.uploadBlob(
      stopExportFilename,
      testResource("Current_latest.zip")
    );
    stopAreaRepositoryFactory.refreshStopAreaRepository(
      testResource("Current_latest.zip")
    );

    gtfsStopExportService.exportStops();

    InputStream gtfsExport = mardukInMemoryBlobStoreRepository.getBlob(
      GTFS_STOP_EXPORT_FILE_NAME
    );
    Assertions.assertNotNull(gtfsExport);

    String gtfsStops = extractGtfsStops(gtfsExport);
    Assertions.assertFalse(gtfsStops.isEmpty());
    Assertions.assertEquals(7, gtfsStops.split("\n").length);
  }

  @Test
  void publishesNothingWhenTheStopDatasetIsMissing() {
    gtfsStopExportService.exportStops();

    Assertions.assertNull(
      mardukInMemoryBlobStoreRepository.getBlob(GTFS_STOP_EXPORT_FILE_NAME)
    );
  }

  private static String extractGtfsStops(InputStream gtfsArchive)
    throws IOException {
    try (ZipInputStream zis = new ZipInputStream(gtfsArchive)) {
      for (
        ZipEntry entry = zis.getNextEntry();
        entry != null;
        entry = zis.getNextEntry()
      ) {
        if (entry.getName().equals("stops.txt")) {
          return new String(zis.readAllBytes());
        }
      }
    }
    return "";
  }
}
