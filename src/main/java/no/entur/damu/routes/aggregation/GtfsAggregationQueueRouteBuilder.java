package no.entur.damu.routes.aggregation;

import static no.entur.damu.Constants.*;
import static org.apache.camel.Exchange.FILE_PARENT;

import java.io.File;
import no.entur.damu.routes.BaseRouteBuilder;
import no.entur.damu.services.MardukBlobStoreService;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Receive a notification with a list of GTFS files to aggregate from marduk's blob store
 */
@Component
public class GtfsAggregationQueueRouteBuilder extends BaseRouteBuilder {

  private static final String ORIGINAL_GTFS_FILES_SUB_FOLDER =
    "/original-gtfs-files";

  @Value("${gtfs.export.download.directory:files/gtfs/merged}")
  private String localWorkingDirectory;

  @Override
  public void configure() throws Exception {
    super.configure();

    onException(Exception.class)
      .handled(true)
      .log(
        LoggingLevel.ERROR,
        correlation() +
        "Dataset processing failed: ${exception.message} stacktrace: ${exception.stacktrace}"
      )
      .setHeader(STATUS_HEADER, constant(STATUS_MERGE_FAILED))
      .log(
        LoggingLevel.INFO,
        "Notifying marduk that aggregation of GTFS has failed"
      )
      .process(new GtfsAggregationStatusProcessor())
      .to(
        "google-pubsub:{{marduk.pubsub.project.id}}:MardukAggregateGtfsStatusQueue"
      )
      .end();

    from("direct:aggregateGtfs")
      .log(
        LoggingLevel.INFO,
        getClass().getName(),
        correlation() + "Starting splitting GTFS files"
      )
      .to("direct:notifyMardukMergeStarted")
      .setHeader(
        FILE_PARENT,
        simple(
          localWorkingDirectory +
          "/EXPORT_GTFS_MERGED/${date:now:yyyyMMddHHmmssSSS}"
        )
      )
      .process(e ->
        new File(
          e.getIn().getHeader(FILE_PARENT, String.class) +
          ORIGINAL_GTFS_FILES_SUB_FOLDER
        )
          .mkdirs()
      )
      .split(body().tokenize(","))
      .to("direct:getGtfsFile")
      .end()
      .process(this::extendAckDeadline)
      .log(
        LoggingLevel.INFO,
        getClass().getName(),
        correlation() + "Done splitting GTFS files"
      )
      .setHeader(
        FILE_HANDLE,
        simple(BLOBSTORE_PATH_OUTBOUND + "gtfs/${exchangeProperty.fileName}")
      )
      .log(LoggingLevel.INFO, "Starting merging of GTFS extended")
      .to("direct:mergeGtfsExtended")
      .process(this::extendAckDeadline)
      .setProperty(FILE_NAME, simple("rb_norway-aggregated-gtfs.zip"))
      .to("direct:uploadMergedGtfs")
      .process(this::extendAckDeadline)
      .log(LoggingLevel.INFO, "Done merging GTFS extended")
      .log(LoggingLevel.INFO, "Starting merging of GTFS basic")
      .to("direct:mergeGtfsBasic")
      .process(this::extendAckDeadline)
      .setProperty(FILE_NAME, simple("rb_norway-aggregated-gtfs-basic.zip"))
      .to("direct:uploadMergedGtfs")
      .process(this::extendAckDeadline)
      .log(LoggingLevel.INFO, "Done merging GTFS basic")
      .log(LoggingLevel.INFO, "Set header to " + constant(STATUS_MERGE_OK))
      .to("direct:notifyMardukMergeOk")
      .to("direct:cleanUpLocalDirectory")
      .process(this::extendAckDeadline)
      .routeId("aggregate-gtfs");

    from("direct:notifyMardukMergeOk")
      .log(
        LoggingLevel.INFO,
        "Notifying marduk that aggregation of GTFS has finished OK"
      )
      .removeHeader(STATUS_HEADER)
      .setHeader(STATUS_HEADER, constant(STATUS_MERGE_OK))
      .process(new GtfsAggregationStatusProcessor())
      .to(
        "google-pubsub:{{marduk.pubsub.project.id}}:MardukAggregateGtfsStatusQueue"
      );

    from("direct:notifyMardukMergeStarted")
      .log(
        LoggingLevel.INFO,
        "Notifying marduk that aggregation of GTFS has started"
      )
      .removeHeader(STATUS_HEADER)
      .setHeader(STATUS_HEADER, constant(STATUS_MERGE_STARTED))
      .process(new GtfsAggregationStatusProcessor())
      .to(
        "google-pubsub:{{marduk.pubsub.project.id}}:MardukAggregateGtfsStatusQueue"
      );

    from("direct:getGtfsFile")
      .log(
        LoggingLevel.INFO,
        getClass().getName(),
        correlation() + "Fetching " + BLOBSTORE_PATH_OUTBOUND + "gtfs/${body}"
      )
      .setProperty("fileName", body())
      .setHeader(
        FILE_HANDLE,
        simple(BLOBSTORE_PATH_OUTBOUND + "gtfs/${exchangeProperty.fileName}")
      )
      .to("direct:getBlob")
      .choice()
      .when(body().isNotEqualTo(null))
      .log(
        LoggingLevel.INFO,
        getClass().getName(),
        correlation() +
        "Fetched ${exchangeProperty.fileName} from blobstore, storing in local directory."
      )
      .toD(
        "file:${header." +
        FILE_PARENT +
        "}" +
        ORIGINAL_GTFS_FILES_SUB_FOLDER +
        "?fileName=${exchangeProperty.fileName}"
      )
      .otherwise()
      .log(
        LoggingLevel.INFO,
        getClass().getName(),
        correlation() +
        "${exchangeProperty.fileName} was empty when trying to fetch it from blobstore."
      )
      .routeId("get-gtfs-file");

    from("direct:mergeGtfsExtended")
      .log(
        LoggingLevel.DEBUG,
        getClass().getName(),
        correlation() + "Merging GTFS extended files for all providers."
      )
      .process(new GtfsExtendedAggregationProcessor())
      .routeId("gtfs-export-merge-extended");

    from("direct:mergeGtfsBasic")
      .log(
        LoggingLevel.DEBUG,
        getClass().getName(),
        correlation() + "Merging GTFS basic files for all providers."
      )
      .process(e -> new GtfsBasicAggregationProcessor(e).process(e))
      .log(
        LoggingLevel.INFO,
        "Done merging GTFS basic files for all providers."
      )
      .routeId("gtfs-export-merge-basic");

    from("direct:uploadMergedGtfs")
      .process(exchange -> {
        log.info("Starting to upload merged GTFS files");
      })
      .setHeader(
        FILE_HANDLE,
        simple(
          BLOBSTORE_PATH_OUTBOUND + "gtfs/${exchangeProperty." + FILE_NAME + "}"
        )
      )
      .to("direct:uploadBlob")
      .log(
        LoggingLevel.INFO,
        getClass().getName(),
        correlation() +
        "Uploaded new merged GTFS file: ${exchangeProperty." +
        FILE_NAME +
        "}"
      )
      .routeId("gtfs-export-upload-merged");
  }
}
