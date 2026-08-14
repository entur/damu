package no.entur.damu.aggregation;

import static no.entur.damu.Constants.BLOBSTORE_PATH_OUTBOUND;
import static no.entur.damu.Constants.CORRELATION_ID;
import static no.entur.damu.Constants.DATASET_REFERENTIAL;
import static no.entur.damu.Constants.ORIGINAL_GTFS_FILES_SUB_FOLDER;
import static no.entur.damu.Constants.STATUS_HEADER;
import static no.entur.damu.Constants.STATUS_MERGE_FAILED;
import static no.entur.damu.Constants.STATUS_MERGE_OK;
import static no.entur.damu.Constants.STATUS_MERGE_STARTED;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import no.entur.damu.DamuMdc;
import no.entur.damu.exception.DamuException;
import no.entur.damu.gtfs.merger.GtfsExport;
import no.entur.damu.gtfs.merger.GtfsFileUtils;
import no.entur.damu.pubsub.DamuQueues;
import no.entur.damu.pubsub.PubSubAttributes;
import no.entur.damu.pubsub.PubSubPublisher;
import no.entur.damu.services.MardukBlobStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

/**
 * Merges the per-provider GTFS exports into the two national datasets.
 */
@Service
public class GtfsAggregationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    GtfsAggregationService.class
  );

  private static final DateTimeFormatter WORKING_DIRECTORY_TIMESTAMP =
    DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

  public static final String AGGREGATED_GTFS_EXTENDED_FILE_NAME =
    "rb_norway-aggregated-gtfs.zip";
  public static final String AGGREGATED_GTFS_BASIC_FILE_NAME =
    "rb_norway-aggregated-gtfs-basic.zip";

  private final MardukBlobStoreService mardukBlobStoreService;
  private final PubSubPublisher pubSubPublisher;
  private final String localWorkingDirectory;
  private final List<String> excludedFromBasicExport;

  public GtfsAggregationService(
    MardukBlobStoreService mardukBlobStoreService,
    PubSubPublisher pubSubPublisher,
    @Value(
      "${gtfs.export.download.directory:files/gtfs/merged}"
    ) String localWorkingDirectory,
    @Value(
      "${damu.gtfs.aggregation.excludedFiles:rb_avi-aggregated-gtfs.zip}"
    ) String excludedFromBasicExport
  ) {
    this.mardukBlobStoreService = mardukBlobStoreService;
    this.pubSubPublisher = pubSubPublisher;
    this.localWorkingDirectory = localWorkingDirectory;
    this.excludedFromBasicExport =
      Arrays.asList(excludedFromBasicExport.split(","));
  }

  /**
   * @param gtfsFileNames the GTFS archives to merge, comma separated, as they arrive in the message
   *                      body.
   * @param requestAttributes the attributes of marduk's request, echoed back on every status
   *                          notification.
   */
  public void aggregate(
    String gtfsFileNames,
    Map<String, String> requestAttributes
  ) {
    Map<String, String> attributes = PubSubAttributes.echo(requestAttributes);
    DamuMdc.setCorrelationId(attributes.get(CORRELATION_ID));
    DamuMdc.setCodespace(attributes.get(DATASET_REFERENTIAL));

    try {
      LOGGER.info("Starting splitting GTFS files");
      notifyMarduk(STATUS_MERGE_STARTED, attributes);

      Path workingDirectory = Path.of(
        localWorkingDirectory,
        "EXPORT_GTFS_MERGED",
        LocalDateTime.now().format(WORKING_DIRECTORY_TIMESTAMP)
      );
      Path originalGtfsFiles = downloadGtfsFiles(
        gtfsFileNames,
        workingDirectory
      );
      LOGGER.info("Done splitting GTFS files");

      LOGGER.info("Starting merging of GTFS extended");
      mergeAndUpload(
        listZipFiles(originalGtfsFiles),
        GtfsExport.GTFS_EXTENDED,
        true,
        AGGREGATED_GTFS_EXTENDED_FILE_NAME
      );
      LOGGER.info("Done merging GTFS extended");

      LOGGER.info("Starting merging of GTFS basic");
      mergeAndUpload(
        basicGtfsFiles(originalGtfsFiles),
        GtfsExport.GTFS_BASIC,
        false,
        AGGREGATED_GTFS_BASIC_FILE_NAME
      );
      LOGGER.info("Done merging GTFS basic");

      notifyMarduk(STATUS_MERGE_OK, attributes);
      // Only on the happy path, as it was under Camel: a failed aggregation leaves its downloads behind.
      deleteDirectoryRecursively(workingDirectory);
    } catch (Exception e) {
      // Every failure is terminal. Camel handled the exception, so the message was acked and never
      // redelivered, and marduk was told the aggregation had failed.
      LOGGER.error("Dataset processing failed: {}", e.getMessage(), e);
      notifyMarduk(STATUS_MERGE_FAILED, attributes);
    }
  }

  /**
   * @return the directory the archives were downloaded into.
   */
  private Path downloadGtfsFiles(String gtfsFileNames, Path workingDirectory) {
    Path originalGtfsFiles = workingDirectory.resolve(
      ORIGINAL_GTFS_FILES_SUB_FOLDER
    );
    try {
      Files.createDirectories(originalGtfsFiles);
    } catch (IOException e) {
      throw new DamuException(
        "Failed to create " + originalGtfsFiles + " for the GTFS aggregation",
        e
      );
    }

    for (String gtfsFileName : gtfsFileNames.split(",")) {
      String fileHandle = BLOBSTORE_PATH_OUTBOUND + "gtfs/" + gtfsFileName;
      LOGGER.info("Fetching {}", fileHandle);
      InputStream gtfsFile = mardukBlobStoreService.getBlob(fileHandle);
      if (gtfsFile == null) {
        LOGGER.info(
          "{} was empty when trying to fetch it from blobstore.",
          gtfsFileName
        );
        continue;
      }
      LOGGER.info(
        "Fetched {} from blobstore, storing in local directory.",
        gtfsFileName
      );
      try (InputStream in = gtfsFile) {
        Files.copy(
          in,
          originalGtfsFiles.resolve(gtfsFileName),
          StandardCopyOption.REPLACE_EXISTING
        );
      } catch (IOException e) {
        throw new DamuException("Failed to store " + gtfsFileName, e);
      }
    }
    return originalGtfsFiles;
  }

  private void mergeAndUpload(
    Collection<File> gtfsFiles,
    GtfsExport gtfsExport,
    boolean includeShapes,
    String aggregatedFileName
  ) {
    try (
      InputStream merged = GtfsFileUtils.mergeGtfsFilesToInputStream(
        gtfsFiles,
        gtfsExport,
        includeShapes
      )
    ) {
      String fileHandle =
        BLOBSTORE_PATH_OUTBOUND + "gtfs/" + aggregatedFileName;
      LOGGER.info("Starting to upload merged GTFS files");
      mardukBlobStoreService.uploadBlob(fileHandle, merged);
      LOGGER.info("Uploaded new merged GTFS file: {}", aggregatedFileName);
    } catch (IOException e) {
      throw new DamuException("Failed to upload " + aggregatedFileName, e);
    }
  }

  /**
   * The basic export leaves out the providers listed in {@code damu.gtfs.aggregation.excludedFiles}.
   */
  private List<File> basicGtfsFiles(Path directory) {
    return listZipFiles(directory)
      .stream()
      .filter(file -> !excludedFromBasicExport.contains(file.getName()))
      .toList();
  }

  private static List<File> listZipFiles(Path directory) {
    if (!Files.isDirectory(directory)) {
      throw new DamuException(directory + " is not a directory");
    }
    try (Stream<Path> files = Files.list(directory)) {
      return files
        .filter(Files::isRegularFile)
        .filter(file -> file.getFileName().toString().endsWith(".zip"))
        .map(Path::toFile)
        .toList();
    } catch (IOException e) {
      throw new DamuException(
        "Failed to list the GTFS files in " + directory,
        e
      );
    }
  }

  /**
   * The body is empty on purpose. Camel sent whatever the exchange was carrying, which was the file
   * list on STARTED and an empty string afterwards; marduk's gtfs-aggregate-status-route reads only the
   * status attribute.
   */
  private void notifyMarduk(String status, Map<String, String> attributes) {
    LOGGER.info("Notifying marduk of aggregation status {}", status);
    Map<String, String> statusAttributes = new LinkedHashMap<>(attributes);
    statusAttributes.put(STATUS_HEADER, status);
    pubSubPublisher.publish(
      DamuQueues.MARDUK_AGGREGATE_GTFS_STATUS_QUEUE,
      "",
      statusAttributes
    );
  }

  private static void deleteDirectoryRecursively(Path directory) {
    LOGGER.debug("Deleting local directory {} ...", directory);
    try {
      if (FileSystemUtils.deleteRecursively(directory)) {
        LOGGER.debug("Local directory {} cleanup done.", directory);
      } else {
        LOGGER.debug(
          "The directory {} did not exist, ignoring deletion request",
          directory
        );
      }
    } catch (IOException e) {
      LOGGER.warn("Failed to delete directory {}", directory, e);
    }
  }
}
