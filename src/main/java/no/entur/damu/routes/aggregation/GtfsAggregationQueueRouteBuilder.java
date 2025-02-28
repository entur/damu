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
import java.util.HashMap;
import java.util.Map;

/**
 * Receive a notification with a list of GTFS files to aggregate from marduk's blob store
 */
@Component
public class GtfsAggregationQueueRouteBuilder extends BaseRouteBuilder {

  private final MardukBlobStoreService mardukPublicBlobStoreService;

  private static final String ORIGINAL_GTFS_FILES_SUB_FOLDER =
    "/original-gtfs-files";

  private static final String STATUS_HEADER = "status";
  private static final String STATUS_MERGE_STARTED = "started";
  private static final String STATUS_MERGE_OK = "ok";
  private static final String STATUS_MERGE_FAILED = "failed";

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
        .to("direct:notifyMardukMerge")
        .end();

    from("google-pubsub:{{damu.pubsub.project.id}}:DamuAggregateGtfsQueue")
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
      .routeId("aggregate-gtfs");

    from("direct:notifyMardukMergeOk")
        .log(LoggingLevel.INFO, "Notifying marduk merge ok "+ constant(STATUS_MERGE_OK))
        .removeHeader(STATUS_HEADER)
        .setHeader(STATUS_HEADER, constant(STATUS_MERGE_OK))
        .process(exchange -> {
            Map<String, String> existingAttributes = exchange.getIn().getHeader("CamelGooglePubsubAttributes", Map.class);
            Map<String, String> nextAttributes = new HashMap<String, String>(existingAttributes);
            String headerValue = exchange.getIn().getHeader(STATUS_HEADER, String.class);
            nextAttributes.put(STATUS_HEADER, headerValue);
            log.info(correlation() + "Notifying marduk of aggregation status " + headerValue);
            exchange.getIn().setHeader("CamelGooglePubsubAttributes", nextAttributes);
        })
        .to("google-pubsub:{{marduk.pubsub.project.id}}:MardukAggregateGtfsStatusQueue");

    from("direct:notifyMardukMergeStarted")
        .log(LoggingLevel.INFO, "Notifying marduk merge started "+ constant(STATUS_MERGE_STARTED))
        .removeHeader(STATUS_HEADER)
        .setHeader(STATUS_HEADER, constant(STATUS_MERGE_STARTED))
        .process(exchange -> {
            Map<String, String> existingAttributes = exchange.getIn().getHeader("CamelGooglePubsubAttributes", Map.class);
            Map<String, String> nextAttributes = new HashMap<String, String>(existingAttributes);
            String headerValue = exchange.getIn().getHeader(STATUS_HEADER, String.class);
            nextAttributes.put(STATUS_HEADER, headerValue);
            log.info(correlation() + "Notifying marduk of aggregation status " + headerValue);
            exchange.getIn().setHeader("CamelGooglePubsubAttributes", nextAttributes);
        })
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
        .process(exchange -> {
          File sourceDirectory = new File(
              exchange.getIn().getHeader(FILE_PARENT, String.class) +
                  ORIGINAL_GTFS_FILES_SUB_FOLDER
          );
          exchange
              .getIn()
              .setBody(
                  GtfsFileUtils.mergeGtfsFilesInDirectory(
                      sourceDirectory,
                      GtfsExport.GTFS_EXTENDED,
                      true
                  )
              );
        })
        .routeId("gtfs-export-merge-extended");

    from("direct:mergeGtfsBasic")
        .log(
            LoggingLevel.DEBUG,
            getClass().getName(),
            correlation() + "Merging GTFS basic files for all providers."
        )
        .process(exchange -> {
          File sourceDirectory = new File(
              exchange.getIn().getHeader(FILE_PARENT, String.class) +
                  ORIGINAL_GTFS_FILES_SUB_FOLDER
          );
          boolean includeShapes =
                exchange.getIn().getHeader(Constants.INCLUDE_SHAPES, Boolean.class);
          exchange
              .getIn()
              .setBody(
                  GtfsFileUtils.mergeGtfsFilesInDirectory(
                      sourceDirectory,
                      GtfsExport.GTFS_BASIC,
                      includeShapes
                  )
              );
        })
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

    from("direct:notifyMardukMerge")
      .process(exchange -> {
          Map<String, String> existingAttributes = exchange.getIn().getHeader("CamelGooglePubsubAttributes", Map.class);
          Map<String, String> nextAttributes = new HashMap<>(existingAttributes);

          String headerValue = exchange.getIn().getHeader(STATUS_HEADER, String.class);
          nextAttributes.put(STATUS_HEADER, headerValue);
          log.info(correlation() + "Notifying marduk of aggregation status " + headerValue);
          exchange.getIn().setHeader("CamelGooglePubsubAttributes", nextAttributes);
      })
     .to("google-pubsub:{{marduk.pubsub.project.id}}:MardukAggregateGtfsStatusQueue")
     .removeHeader(STATUS_HEADER)
    .routeId("notify-marduk-merge");
  }
}
