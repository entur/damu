package no.entur.damu.validation;

import static no.entur.damu.Constants.GTFS_VALIDATION_REPORTS_FILENAME_PREFIX;
import static no.entur.damu.Constants.GTFS_VALIDATION_REPORTS_FILENAME_SUFFIX;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import no.entur.damu.exception.DamuException;
import no.entur.damu.gtfs.validator.GtfsValidator;
import no.entur.damu.services.DamuBlobStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Runs the MobilityData GTFS validator over an exported dataset and stores the reports.
 */
@Service
public class GtfsValidationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    GtfsValidationService.class
  );

  private final DamuBlobStoreService damuBlobStoreService;

  public GtfsValidationService(DamuBlobStoreService damuBlobStoreService) {
    this.damuBlobStoreService = damuBlobStoreService;
  }

  public void validateAndUploadReports(String codespace, Path gtfsDataset) {
    String fileHandle =
      GTFS_VALIDATION_REPORTS_FILENAME_PREFIX +
      codespace +
      GTFS_VALIDATION_REPORTS_FILENAME_SUFFIX;
    try (InputStream gtfs = Files.newInputStream(gtfsDataset)) {
      LOGGER.info("Validating GTFS dataset");
      InputStream reports = GtfsValidator.validate(codespace, gtfs);
      LOGGER.info(
        "GTFS validation complete, uploading reports to GCS file {}",
        fileHandle
      );
      damuBlobStoreService.uploadBlob(fileHandle, reports);
      LOGGER.info(
        "Uploaded GTFS validation reports to GCS file {}",
        fileHandle
      );
    } catch (IOException e) {
      throw new DamuException(
        "Failed to validate the GTFS dataset for " + codespace,
        e
      );
    }
  }
}
