package no.entur.damu.export;

import static no.entur.damu.Constants.BLOBSTORE_PATH_OUTBOUND;
import static no.entur.damu.Constants.CORRELATION_ID;
import static no.entur.damu.Constants.DATASET_REFERENTIAL;
import static no.entur.damu.Constants.GTFS_FILENAME_PREFIX;
import static no.entur.damu.Constants.GTFS_FILENAME_SUFFIX;
import static no.entur.damu.Constants.NETEX_FILENAME_PREFIX;
import static no.entur.damu.Constants.NETEX_FILENAME_SUFFIX;
import static no.entur.damu.Constants.STATUS_EXPORT_FAILED;
import static no.entur.damu.Constants.STATUS_EXPORT_OK;
import static no.entur.damu.Constants.STATUS_EXPORT_STARTED;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import no.entur.damu.DamuMdc;
import no.entur.damu.exception.DamuException;
import no.entur.damu.netex.EnturGtfsExporter;
import no.entur.damu.pubsub.DamuQueues;
import no.entur.damu.pubsub.PubSubAttributes;
import no.entur.damu.pubsub.PubSubPublisher;
import no.entur.damu.services.MardukBlobStoreService;
import no.entur.damu.validation.GtfsValidationService;
import org.entur.netex.gtfs.export.GtfsExporter;
import org.entur.netex.gtfs.export.exception.GtfsExportException;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Converts a NeTEx export into a GTFS dataset when marduk says a new one is available.
 */
@Service
public class GtfsExportService {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    GtfsExportService.class
  );

  /**
   * java.io.tmpdir is world-writable, so the GTFS dataset is created owner-only rather than with the
   * platform default. The codespace is deliberately not part of the name: it comes off the message and
   * a temp file prefix is not the place to sanitise it.
   */
  private static final FileAttribute<Set<PosixFilePermission>> OWNER_ONLY =
    PosixFilePermissions.asFileAttribute(
      PosixFilePermissions.fromString("rw-------")
    );

  private final MardukBlobStoreService mardukBlobStoreService;
  private final GtfsValidationService gtfsValidationService;
  private final StopAreaRepositoryFactory stopAreaRepositoryFactory;
  private final PubSubPublisher pubSubPublisher;
  private final String gtfsExportFolder;
  private final boolean generateStaySeatedTransfer;

  public GtfsExportService(
    MardukBlobStoreService mardukBlobStoreService,
    GtfsValidationService gtfsValidationService,
    StopAreaRepositoryFactory stopAreaRepositoryFactory,
    PubSubPublisher pubSubPublisher,
    @Value("${damu.gtfs.export.folder:damu}") String gtfsExportFolder,
    @Value(
      "${damu.gtfs.export.transfer.stayseated:false}"
    ) boolean generateStaySeatedTransfer
  ) {
    this.mardukBlobStoreService = mardukBlobStoreService;
    this.gtfsValidationService = gtfsValidationService;
    this.stopAreaRepositoryFactory = stopAreaRepositoryFactory;
    this.pubSubPublisher = pubSubPublisher;
    this.gtfsExportFolder = gtfsExportFolder;
    this.generateStaySeatedTransfer = generateStaySeatedTransfer;
  }

  /**
   * @param codespace the dataset referential, which arrives as the message body.
   * @param requestAttributes the attributes of marduk's request. They are echoed back on every status
   *                          notification, which is how marduk matches the status to its pending job.
   */
  public void export(String codespace, Map<String, String> requestAttributes) {
    Map<String, String> attributes = PubSubAttributes.echo(requestAttributes);
    attributes.putIfAbsent(CORRELATION_ID, UUID.randomUUID().toString());
    attributes.put(DATASET_REFERENTIAL, codespace);
    DamuMdc.setCorrelationId(attributes.get(CORRELATION_ID));
    DamuMdc.setCodespace(codespace);

    LOGGER.info("Received GTFS export request");
    notifyMarduk(STATUS_EXPORT_STARTED, attributes);

    try {
      InputStream netexTimetableDataset = downloadNetexTimetableDataset(
        codespace
      );
      if (netexTimetableDataset == null) {
        // Camel stopped the exchange here, leaving marduk with a STARTED and no terminal status.
        // Preserved on purpose: changing it changes what marduk records for the provider.
        return;
      }
      Path gtfsDataset = convertToGtfs(codespace, netexTimetableDataset);
      try {
        validateAndUpload(codespace, gtfsDataset);
      } finally {
        deleteQuietly(gtfsDataset);
      }
      notifyMarduk(STATUS_EXPORT_OK, attributes);
    } catch (GtfsExportException e) {
      // The only handled exception. Anything else propagates out of the consumer callback, which nacks
      // the message and lets PubSub redeliver it.
      LOGGER.error("Dataset processing failed: {}", e.getMessage(), e);
      notifyMarduk(STATUS_EXPORT_FAILED, attributes);
    }
  }

  private InputStream downloadNetexTimetableDataset(String codespace) {
    LOGGER.info("Downloading NeTEx Timetable dataset");
    InputStream dataset = mardukBlobStoreService.getBlob(
      BLOBSTORE_PATH_OUTBOUND +
      NETEX_FILENAME_PREFIX +
      codespace +
      NETEX_FILENAME_SUFFIX
    );
    if (dataset == null) {
      LOGGER.error("NeTEx Timetable file not found");
      return null;
    }
    LOGGER.info("NeTEx Timetable file downloaded");
    return dataset;
  }

  /**
   * The exporter hands back a stream over a temp file it deletes on close, so it can only be read once.
   * Copying it to a file of our own is what lets validation and upload both read it; under Camel that
   * was stream caching, enabled per route and spooling to disk.
   */
  Path convertToGtfs(String codespace, InputStream netexTimetableDataset) {
    LOGGER.info("Converting to GTFS");
    GtfsExporter gtfsExporter = new EnturGtfsExporter(
      codespace.replace("rb_", "").toUpperCase(Locale.ROOT),
      stopAreaRepositoryFactory.getStopAreaRepository(),
      generateStaySeatedTransfer
    );
    try (
      InputStream gtfs = gtfsExporter.convertTimetablesToGtfs(
        netexTimetableDataset
      )
    ) {
      Path gtfsDataset = Files.createTempFile("damu-gtfs-", ".zip", OWNER_ONLY);
      try {
        Files.copy(gtfs, gtfsDataset, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException | RuntimeException e) {
        // The caller only deletes what this method returns, so a half-written file would stay in
        // java.io.tmpdir. A full disk fails here every time and would fill it further.
        deleteQuietly(gtfsDataset);
        throw e;
      }
      LOGGER.info("Dataset processing complete");
      return gtfsDataset;
    } catch (IOException e) {
      throw new DamuException(
        "Failed to store the GTFS dataset for " + codespace,
        e
      );
    }
  }

  /**
   * Camel ran these as a sequential multicast with {@code stopOnException} left at its default, so the
   * upload happened even when validation had already failed, and the failure was propagated afterwards.
   */
  private void validateAndUpload(String codespace, Path gtfsDataset) {
    RuntimeException validationFailure = null;
    try {
      gtfsValidationService.validateAndUploadReports(codespace, gtfsDataset);
    } catch (RuntimeException e) {
      validationFailure = e;
    }
    uploadGtfsDataset(codespace, gtfsDataset);
    if (validationFailure != null) {
      throw validationFailure;
    }
  }

  private void uploadGtfsDataset(String codespace, Path gtfsDataset) {
    String fileHandle =
      gtfsExportFolder +
      '/' +
      GTFS_FILENAME_PREFIX +
      codespace +
      GTFS_FILENAME_SUFFIX;
    LOGGER.info("Uploading GTFS file to GCS file {}", fileHandle);
    try (InputStream gtfs = Files.newInputStream(gtfsDataset)) {
      mardukBlobStoreService.uploadBlob(fileHandle, gtfs);
    } catch (IOException e) {
      throw new DamuException("Failed to upload " + fileHandle, e);
    }
    LOGGER.info("Uploaded GTFS file to GCS file {}", fileHandle);
  }

  private void notifyMarduk(String status, Map<String, String> attributes) {
    LOGGER.info("Notifying marduk of export status {}", status);
    pubSubPublisher.publish(
      DamuQueues.DAMU_EXPORT_GTFS_STATUS_QUEUE,
      status,
      attributes
    );
  }

  private static void deleteQuietly(Path file) {
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      LOGGER.warn("Failed to delete {}", file, e);
    }
  }
}
