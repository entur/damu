package no.entur.damu.routes.aggregation;

import static no.entur.damu.Constants.*;
import static org.apache.camel.Exchange.FILE_PARENT;

import no.entur.damu.Constants;
import no.entur.damu.gtfs.merger.GtfsExport;
import no.entur.damu.gtfs.merger.GtfsFileUtils;
import no.entur.damu.routes.BaseRouteBuilder;
import no.entur.damu.services.MardukBlobStoreService;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Receive a notification with a list of GTFS files to aggregate from marduk's blob store
 */
@Component
public class GtfsAggregationQueueRouteBuilder extends BaseRouteBuilder {

  private final MardukBlobStoreService mardukPublicBlobStoreService;

  private static final String ORIGINAL_GTFS_FILES_SUB_FOLDER =
    "/original-gtfs-files";

  @Value("${gtfs.export.download.directory:files/gtfs/merged}")
  private String localWorkingDirectory;

  public GtfsAggregationQueueRouteBuilder(
    MardukBlobStoreService mardukPublicBlobStoreService
  ) {
    super();
    this.mardukPublicBlobStoreService = mardukPublicBlobStoreService;
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    // TODO(eibakke): introduce better exception handling with custom exception types
    onException(Exception.class)
        .log(
            LoggingLevel.ERROR,
            correlation() +
                "Dataset processing failed: ${exception.message} stacktrace: ${exception.stacktrace}"
        )
        .setHeader(STATUS_HEADER, constant(STATUS_MERGE_FAILED))
        .log(LoggingLevel.INFO, "Notifying marduk that aggregation of GTFS has failed")
        .process(new GtfsAggregationStatusProcessor())
        .end();

    from("google-pubsub:{{marduk.pubsub.project.id}}:DamuAggregateGtfsQueue")
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
          "/${header." +
          JOB_ACTION +
          "}/${date:now:yyyyMMddHHmmssSSS}"
        )
      )
      .split(body().tokenize(","))
      .to("direct:getGtfsFile")
      .end()
      .log(
        LoggingLevel.INFO,
        getClass().getName(),
        correlation() + "Done splitting GTFS files"
      )
      .setHeader(
        FILE_HANDLE,
        simple(BLOBSTORE_PATH_OUTBOUND + "gtfs/${exchangeProperty.fileName}")
      )
      .choice()
          .when(exchange -> exchange.getIn().getHeader(JOB_ACTION) == null)
              .log(
                LoggingLevel.ERROR,
                getClass().getName(),
                correlation() + "Missing JOB_ACTION header in pubsub message"
              )
          .stop()
          .when(exchange -> exchange.getIn().getHeader(JOB_ACTION).equals("EXPORT_GTFS_MERGED"))
            .log(LoggingLevel.INFO, "Starting merging of GTFS extended")
            .to("direct:mergeGtfsExtended")
          .when(exchange -> exchange.getIn().getHeader(JOB_ACTION).equals("EXPORT_GTFS_BASIC_MERGED"))
            .log(LoggingLevel.INFO, "Starting merging of GTFS basic")
            .to("direct:mergeGtfsBasic")
      .end()
      .to("direct:uploadMergedGtfs")
      .log(LoggingLevel.INFO, "Set header to "+ constant(STATUS_MERGE_OK))
      .to("direct:notifyMardukMergeOk")
      .to("direct:cleanUpLocalDirectory")
      .routeId("aggregate-gtfs");

    from("direct:notifyMardukMergeOk")
        .log(LoggingLevel.INFO, "Notifying marduk that aggregation of GTFS has finished OK")
        .removeHeader(STATUS_HEADER)
        .setHeader(STATUS_HEADER, constant(STATUS_MERGE_OK))
        .process(new GtfsAggregationStatusProcessor())
        .to("google-pubsub:{{marduk.pubsub.project.id}}:MardukAggregateGtfsStatusQueue");

    from("direct:notifyMardukMergeStarted")
        .log(LoggingLevel.INFO, "Notifying marduk that aggregation of GTFS has started")
        .removeHeader(STATUS_HEADER)
        .setHeader(STATUS_HEADER, constant(STATUS_MERGE_STARTED))
        .process(new GtfsAggregationStatusProcessor())
        .to("google-pubsub:{{marduk.pubsub.project.id}}:MardukAggregateGtfsStatusQueue");

    from("direct:getGtfsFile")
        .log(
            LoggingLevel.INFO,
            getClass().getName(),
            correlation() + "Fetching " + BLOBSTORE_PATH_OUTBOUND + "gtfs/${body}"
        )
        .process(e -> new File(e.getIn().getHeader(FILE_PARENT, String.class) + ORIGINAL_GTFS_FILES_SUB_FOLDER).mkdirs())
        .setProperty("fileName", body())
        .setHeader(
            FILE_HANDLE,
            simple(BLOBSTORE_PATH_OUTBOUND + "gtfs/${exchangeProperty.fileName}")
        )
        .to("direct:getBlob")
        .choice()
        .when(body().isNotEqualTo(null))
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
        .to(logDebugShowAll())
        .bean(mardukPublicBlobStoreService, "getBlob")
        .to(logDebugShowAll())
        .log(
            LoggingLevel.INFO,
            correlation() +
                "Returning from fetching file ${header." +
                FILE_HANDLE +
                "} from blob store."
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
        .process(new GtfsBasicAggregationProcessor())
        .routeId("gtfs-export-merge-basic");

    from("direct:uploadMergedGtfs")
        .process(exchange -> {
          log.info("Starting to upload merged GTFS files");
        })
        .setHeader(
            FILE_HANDLE,
            simple(BLOBSTORE_PATH_OUTBOUND + "gtfs/${header." + FILE_NAME + "}")
        )
        .to("direct:uploadBlob")
        .log(
            LoggingLevel.INFO,
            getClass().getName(),
            correlation() +
                "Uploaded new merged GTFS file: ${header." +
                FILE_NAME +
                "}"
        )
        .routeId("gtfs-export-upload-merged");
  }
}
