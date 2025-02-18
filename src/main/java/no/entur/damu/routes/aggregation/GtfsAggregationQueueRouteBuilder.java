package no.entur.damu.routes.aggregation;

import no.entur.damu.routes.BaseRouteBuilder;
import no.entur.damu.services.MardukBlobStoreService;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.entur.damu.Constants.*;
import static org.apache.camel.Exchange.FILE_PARENT;

/**
 * Receive a notification with a list of GTFS files to aggregate from marduk's blob store
 */
@Component
public class GtfsAggregationQueueRouteBuilder extends BaseRouteBuilder {
    private final MardukBlobStoreService mardukPublicBlobStoreService;

    private static final String ORIGINAL_GTFS_FILES_SUB_FOLDER = "/original-gtfs-files";

    @Value("${gtfs.export.download.directory:files/gtfs/merged}")
    private String localWorkingDirectory;

    public GtfsAggregationQueueRouteBuilder(MardukBlobStoreService mardukPublicBlobStoreService) {
        super();
        this.mardukPublicBlobStoreService = mardukPublicBlobStoreService;
    }

    @Override
    public void configure() throws Exception {
        super.configure();

        from("google-pubsub:{{damu.pubsub.project.id}}:DamuAggregateGtfsQueue")
            .log(
                LoggingLevel.INFO,
                getClass().getName(),
                correlation() + "Starting splitting GTFS files"
            )
            .setHeader(FILE_PARENT, simple(localWorkingDirectory + "/${header." + JOB_ACTION + "}/${date:now:yyyyMMddHHmmssSSS}"))
            .split(body().tokenize(","))
                .to("direct:getGtfsFile")
            .end()
            .log(
                LoggingLevel.INFO,
                getClass().getName(),
                correlation() + "Done splitting GTFS files"
            )
            .setHeader(FILE_HANDLE, simple(BLOBSTORE_PATH_OUTBOUND + "gtfs/${exchangeProperty.fileName}"))
            .setHeader(JOB_ACTION, simple(EXPORT_GTFS_MERGED))
            .to("direct:mergeGtfs")
            .to("direct:uploadMergedGtfs")
            .routeId("aggregate-gtfs");

        from("direct:getGtfsFile")
            .log(
                LoggingLevel.INFO,
                getClass().getName(),
                correlation() + "Fetching " + BLOBSTORE_PATH_OUTBOUND + "gtfs/${body}"
            )
            .setProperty("fileName", body())
            .setHeader(FILE_HANDLE, simple(BLOBSTORE_PATH_OUTBOUND + "gtfs/${exchangeProperty.fileName}"))
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
            .log(LoggingLevel.INFO, correlation() + "Returning from fetching file ${header." + FILE_HANDLE + "} from blob store.")
            .routeId("get-gtfs-file");
    }
}
