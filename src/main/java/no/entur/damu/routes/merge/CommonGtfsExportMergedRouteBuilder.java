/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package no.entur.damu.routes.merge;

import no.entur.damu.Constants;
import no.entur.damu.gtfs.merger.GtfsExport;
import no.entur.damu.gtfs.merger.GtfsFileUtils;
import no.entur.damu.routes.BaseRouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.entur.damu.Constants.*;
import static org.apache.camel.Exchange.FILE_PARENT;

/**
 * Common routes for building GTFS exports.
 */
@Component
public class CommonGtfsExportMergedRouteBuilder extends BaseRouteBuilder {

    private static final String STATUS_MERGE_STARTED = "started";
    private static final String STATUS_MERGE_OK = "ok";
    private static final String STATUS_MERGE_FAILED = "failed";

    private static final String ORIGINAL_GTFS_FILES_SUB_FOLDER = "/original-gtfs-files";

    @Value("${gtfs.export.download.directory:files/gtfs/merged}")
    private String localWorkingDirectory;

    @Override
    public void configure() throws Exception {
        super.configure();

        from("direct:exportMergedGtfs")
                .log(LoggingLevel.INFO, getClass().getName(), correlation() + "Start export of merged GTFS file: ${header." + FILE_NAME + "}")
                .setBody(constant(STATUS_MERGE_STARTED))
                .to("direct:notifyMardukMerge")
                .setHeader(FILE_PARENT, simple(localWorkingDirectory + "/${header." + JOB_ACTION + "}/${date:now:yyyyMMddHHmmssSSS}"))
                .doTry()
                .to("direct:fetchLatestGtfs")
                .to("direct:mergeGtfs")
                .to("direct:uploadMergedGtfs")

                .setBody(constant(STATUS_MERGE_OK))
                .to("direct:notifyMardukMerge")
                // Use wire tap to avoid replacing body
                .log(LoggingLevel.INFO, getClass().getName(), correlation() + "Completed export of merged GTFS file: ${header." + FILE_NAME + "}")
                .doFinally()
                .to("direct:cleanUpLocalDirectory")
                .end()
                .routeId("gtfs-export-merged-route");


        from("direct:fetchLatestGtfs")
                .log(LoggingLevel.DEBUG, getClass().getName(), correlation() + "Fetching gtfs files for all providers.")
                .process(e -> new File(e.getIn().getHeader(FILE_PARENT, String.class) + ORIGINAL_GTFS_FILES_SUB_FOLDER).mkdirs())
                .process(e -> e.getIn().setBody(getAggregatedGtfsFiles(getProviderBlackList(e), getProviderWhiteList(e))))
                .choice().when(simple("${body.empty}"))
                .log(LoggingLevel.INFO, getClass().getName(), correlation() + "No gtfs files configured for inclusion in export '${exchangeProperty.fileName}', terminating export.")
                .stop()
                .end()
                .split(body())
                .to("direct:getGtfsFiles")
                .routeId("gtfs-export-fetch-latest");

        from("direct:getGtfsFiles")
                .log(LoggingLevel.INFO, getClass().getName(), correlation() + "Fetching " + BLOBSTORE_PATH_OUTBOUND + "gtfs/${body}")
                .setProperty("fileName", body())
                .setHeader(FILE_HANDLE, simple(BLOBSTORE_PATH_OUTBOUND + "gtfs/${exchangeProperty.fileName}"))
                .to("direct:getBlob")
                .choice()
                .when(body().isNotEqualTo(null))
                .toD("file:${header." + FILE_PARENT + "}" + ORIGINAL_GTFS_FILES_SUB_FOLDER + "?fileName=${exchangeProperty.fileName}")
                .otherwise()
                .log(LoggingLevel.INFO, getClass().getName(), correlation() + "${exchangeProperty.fileName} was empty when trying to fetch it from blobstore.")
                .routeId("gtfs-export-get-latest-for-provider");

        from("direct:mergeGtfs")
                .log(LoggingLevel.DEBUG, getClass().getName(), correlation() + "Merging gtfs files for all providers.")

                .process(exchange ->
                        {
                            File sourceDirectory = new File(exchange.getIn().getHeader(FILE_PARENT, String.class) + ORIGINAL_GTFS_FILES_SUB_FOLDER);
                            String jobAction = exchange.getIn().getHeader(Constants.JOB_ACTION, String.class);
                            boolean includeShapes= true;
                            GtfsExport gtfsExport = null;
                            if (EXPORT_GTFS_MERGED.equals(jobAction)) {
                                gtfsExport = GtfsExport.GTFS_EXTENDED;
                            } else if (EXPORT_GTFS_BASIC_MERGED.equals(jobAction)) {
                                includeShapes =  exchange.getIn().getHeader(Constants.INCLUDE_SHAPES, Boolean.class);
                                gtfsExport = GtfsExport.GTFS_BASIC;
                            }
                            exchange.getIn().setBody(GtfsFileUtils.mergeGtfsFilesInDirectory(sourceDirectory, gtfsExport, includeShapes));
                        }
                )
                .routeId("gtfs-export-merge");

        from("direct:uploadMergedGtfs")
                .setHeader(FILE_HANDLE, simple(BLOBSTORE_PATH_OUTBOUND + "gtfs/${header." + FILE_NAME + "}"))
                .to("direct:uploadBlob")
                .log(LoggingLevel.INFO, getClass().getName(), correlation() + "Uploaded new merged GTFS file: ${header." + FILE_NAME + "}")
                .routeId("gtfs-export-upload-merged");

        from("direct:notifyMardukMerge")
            .to("google-pubsub:{{damu.pubsub.project.id}}:DamuMergeGtfsStatusQueue")
            .routeId("notify-marduk");
    }

    private List<String> getAggregatedGtfsFiles(Collection<String> providerBlackList, Collection<String> providerWhiteList) {
        return getProviderRepository().getProviders().stream()
                                 .filter(p -> p.getChouetteInfo().getMigrateDataToProvider() == null)
                                 .filter(p -> isMatch(p, providerBlackList, providerWhiteList))
                                 .map(p -> p.getChouetteInfo().getReferential() + "-" + CURRENT_AGGREGATED_GTFS_FILENAME)
                                 .toList();
    }

    private boolean isMatch(Provider p, Collection<String> providerBlackList, Collection<String> providerWhiteList) {
        if (providerWhiteList == null) {
            return providerBlackList.stream().noneMatch(blacklisted -> blacklisted.equalsIgnoreCase(p.getChouetteInfo().getReferential()));
        }
        return providerWhiteList.stream().anyMatch(whiteListed -> whiteListed.equalsIgnoreCase(p.getChouetteInfo().getReferential()));
    }

    private Collection<String> getProviderBlackList(Exchange e) {
        return Optional.ofNullable(e.getProperty(PROVIDER_BLACK_LIST, Collection.class)).orElse(Collections.emptyList());
    }

    private Collection<String> getProviderWhiteList(Exchange e) {
        return e.getProperty(PROVIDER_WHITE_LIST, Collection.class);
    }
}

