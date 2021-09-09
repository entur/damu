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

package no.entur.damu.routes.export;

import no.entur.damu.Constants;
import no.entur.damu.export.exception.GtfsExportException;
import no.entur.damu.routes.BaseRouteBuilder;
import no.entur.damu.export.GtfsExport;
import no.entur.damu.export.stop.StopAreaRepositoryFactory;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import java.io.InputStream;

import static no.entur.damu.Constants.BLOBSTORE_PATH_DAMU;
import static no.entur.damu.Constants.BLOBSTORE_PATH_OUTBOUND;
import static no.entur.damu.Constants.DATASET_CODESPACE;
import static no.entur.damu.Constants.FILE_HANDLE;

/**
 * Receive a notification when a new NeTEx export is available in the blob store and convert it into a GTFS dataset.
 */
@Component
public class GtfsExportQueueRouteBuilder extends BaseRouteBuilder {

    private static final String TIMETABLE_EXPORT_FILE_NAME = BLOBSTORE_PATH_OUTBOUND + Constants.NETEX_FILENAME_PREFIX + "${header." + DATASET_CODESPACE + "}" + Constants.NETEX_FILENAME_SUFFIX;
    private static final String GTFS_EXPORT_FILE_NAME = BLOBSTORE_PATH_DAMU + Constants.GTFS_FILENAME_PREFIX + "${header." + DATASET_CODESPACE + "}" + Constants.GTFS_FILENAME_SUFFIX;

    private static final String TIMETABLE_DATASET_FILE = "TIMETABLE_DATASET_FILE";

    private final StopAreaRepositoryFactory stopAreaRepositoryFactory;

    public GtfsExportQueueRouteBuilder(StopAreaRepositoryFactory stopAreaRepositoryFactory) {
        super();
        this.stopAreaRepositoryFactory = stopAreaRepositoryFactory;
    }

    @Override
    public void configure() throws Exception {
        super.configure();


        from("master:lockOnDamuExportGtfsQueueRoute:google-pubsub:{{damu.pubsub.project.id}}:DamuExportGtfsQueue?synchronousPull=true")

                .process(this::setCorrelationIdIfMissing)
                .setHeader(DATASET_CODESPACE, bodyAs(String.class))
                .log(LoggingLevel.INFO, correlation() + "Received GTFS export request")

                .to("direct:downloadNetexTimetableDataset")
                .log(LoggingLevel.INFO, correlation() + "NeTEx Timetable file downloaded")
                .setHeader(TIMETABLE_DATASET_FILE, body())

                .to("direct:convertToGtfs")
                .to("direct:uploadGtfsDataset")
                .routeId("gtfs-export-queue");

        from("direct:downloadNetexTimetableDataset")
                .log(LoggingLevel.INFO, correlation() + "Downloading NeTEx Timetable dataset")
                .setHeader(FILE_HANDLE, simple(TIMETABLE_EXPORT_FILE_NAME))
                .to("direct:getMardukBlob")
                .filter(body().isNull())
                .log(LoggingLevel.ERROR, correlation() + "NeTEx Timetable file not found")
                .stop()
                //end filter
                .end()
                .routeId("download-netex-timetable-dataset");


        from("direct:convertToGtfs")
                .log(LoggingLevel.INFO, correlation() + "Converting to GTFS")
                .doTry()
                .process(exchange -> {
                    InputStream timetableDataset = exchange.getIn().getHeader(TIMETABLE_DATASET_FILE, InputStream.class);
                    GtfsExport gtfsExport = new GtfsExport(timetableDataset, stopAreaRepositoryFactory.getStopAreaRepository());
                    exchange.getIn().setBody(gtfsExport.exportGtfs());
                })
                .log(LoggingLevel.INFO, correlation() + "Dataset processing complete")
                .doCatch(GtfsExportException.class)
                .log(LoggingLevel.ERROR, correlation() + "Dataset processing failed: ${exception.message} stacktrace: ${exception.stacktrace}")
                .end()
                .routeId("convert-to-gtfs");

        from("direct:uploadGtfsDataset")
                .setHeader(FILE_HANDLE, simple(GTFS_EXPORT_FILE_NAME))
                .log(LoggingLevel.INFO, correlation() + "Uploading GTFS file " + GTFS_EXPORT_FILE_NAME + " to GCS file ${header." + FILE_HANDLE + "}")
                .to("direct:uploadMardukBlob")
                .log(LoggingLevel.INFO, correlation() + "Uploaded GTFS file " + GTFS_EXPORT_FILE_NAME + " to GCS file ${header." + FILE_HANDLE + "}")
                .routeId("upload-gtfs-dataset");

    }

}
