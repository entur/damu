package no.entur.damu.export;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import no.entur.damu.DamuMdc;
import no.entur.damu.exception.DamuException;
import no.entur.damu.netex.EnturGtfsExporter;
import no.entur.damu.services.MardukBlobStoreService;
import org.entur.netex.gtfs.export.GtfsExporter;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Publishes a GTFS export of the stop places on a schedule.
 */
@Service
public class GtfsStopExportService {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    GtfsStopExportService.class
  );

  public static final String GTFS_STOP_EXPORT_FILE_NAME =
    "tiamat/Current_latest-gtfs.zip";

  private final MardukBlobStoreService mardukBlobStoreService;
  private final StopAreaRepositoryFactory stopAreaRepositoryFactory;
  private final String stopExportFilename;

  public GtfsStopExportService(
    MardukBlobStoreService mardukBlobStoreService,
    StopAreaRepositoryFactory stopAreaRepositoryFactory,
    @Value(
      "${damu.netex.stop.current.filename:tiamat/Current_latest.zip}"
    ) String stopExportFilename
  ) {
    this.mardukBlobStoreService = mardukBlobStoreService;
    this.stopAreaRepositoryFactory = stopAreaRepositoryFactory;
    this.stopExportFilename = stopExportFilename;
  }

  @Scheduled(cron = "${damu.netex.stop.export.cron:0 30 3 * * *}")
  public void exportStops() {
    DamuMdc.clear();
    DamuMdc.setCorrelationId(UUID.randomUUID().toString());

    if (!currentStopsDatasetExists()) {
      return;
    }
    uploadCurrentStopsGtfsDataset(convertCurrentStopsToGtfs());
  }

  /**
   * The dataset itself is never read: the conversion below runs off the stop area repository, which is
   * loaded from a different archive. The check is the only thing standing between an unpublished stop
   * export and a published empty one.
   */
  private boolean currentStopsDatasetExists() {
    if (mardukBlobStoreService.exists(stopExportFilename)) {
      return true;
    }
    LOGGER.error("NeTEx Stopfile not found");
    return false;
  }

  private InputStream convertCurrentStopsToGtfs() {
    LOGGER.info("Converting Current Stops to GTFS");
    GtfsExporter gtfsExporter = new EnturGtfsExporter(
      stopAreaRepositoryFactory.getStopAreaRepository()
    );
    InputStream gtfs = gtfsExporter.convertStopsToGtfs();
    LOGGER.info("Converted Current Stops to GTFS");
    return gtfs;
  }

  private void uploadCurrentStopsGtfsDataset(InputStream gtfs) {
    LOGGER.info(
      "Uploading GTFS file to GCS file {}",
      GTFS_STOP_EXPORT_FILE_NAME
    );
    try (InputStream in = gtfs) {
      mardukBlobStoreService.uploadBlob(GTFS_STOP_EXPORT_FILE_NAME, in);
    } catch (IOException e) {
      throw new DamuException(
        "Failed to upload " + GTFS_STOP_EXPORT_FILE_NAME,
        e
      );
    }
    LOGGER.info(
      "Uploaded GTFS file to GCS file {}",
      GTFS_STOP_EXPORT_FILE_NAME
    );
  }
}
