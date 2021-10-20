/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *  *
 *
 */

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
import org.entur.netex.gtfs.export.GtfsExporter;
import org.entur.netex.gtfs.export.exception.GtfsExportException;
import no.entur.damu.netex.EnturGtfsExporter;
import no.entur.damu.routes.BaseRouteBuilder;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

import static no.entur.damu.Constants.BLOBSTORE_PATH_OUTBOUND;
import static no.entur.damu.Constants.DATASET_CODESPACE;
import static no.entur.damu.Constants.FILE_HANDLE;

/**
 * Receive a notification when a new NeTEx export is available in the blob store and convert it into a GTFS dataset.
 */
@Component
public class GtfsExportQueueRouteBuilder extends BaseRouteBuilder {

    private static final String TIMETABLE_EXPORT_FILE_NAME = BLOBSTORE_PATH_OUTBOUND + Constants.NETEX_FILENAME_PREFIX + "${header." + DATASET_CODESPACE + "}" + Constants.NETEX_FILENAME_SUFFIX;
    private static final String GTFS_EXPORT_FILE_NAME = Constants.GTFS_FILENAME_PREFIX + "${header." + DATASET_CODESPACE + "}" + Constants.GTFS_FILENAME_SUFFIX;

    private static final String TIMETABLE_DATASET_FILE = "TIMETABLE_DATASET_FILE";

    private static final String STATUS_EXPORT_STARTED = "started";
    private static final String STATUS_EXPORT_OK = "ok";
    private static final String STATUS_EXPORT_FAILED= "failed";

    private final StopAreaRepositoryFactory stopAreaRepositoryFactory;

    private final String gtfsExportFilePath;

    public GtfsExportQueueRouteBuilder(StopAreaRepositoryFactory stopAreaRepositoryFactory, @Value("${damu.gtfs.export.folder:damu}") String gtfsExportFolder) {
        super();
        this.stopAreaRepositoryFactory = stopAreaRepositoryFactory;
        this.gtfsExportFilePath = gtfsExportFolder + '/' + GTFS_EXPORT_FILE_NAME;
    }

    @Override
    public void configure() throws Exception {
        super.configure();


        from("google-pubsub:{{damu.pubsub.project.id}}:DamuExportGtfsQueue")

                .process(this::setCorrelationIdIfMissing)
                .setHeader(DATASET_CODESPACE, bodyAs(String.class))
                .log(LoggingLevel.INFO, correlation() + "Received GTFS export request")

                .setBody(constant(STATUS_EXPORT_STARTED))
                .to("direct:notifyMarduk")

                .doTry()
                .to("direct:downloadNetexTimetableDataset")
                .log(LoggingLevel.INFO, correlation() + "NeTEx Timetable file downloaded")
                .setHeader(TIMETABLE_DATASET_FILE, body())
                .to("direct:convertToGtfs")
                .to("direct:uploadGtfsDataset")
                .setBody(constant(STATUS_EXPORT_OK))
                .to("direct:notifyMarduk")
                // catching only GtfsExportException. They are generally not retryable.
                .doCatch(GtfsExportException.class)
                .log(LoggingLevel.ERROR, correlation() + "Dataset processing failed: ${exception.message} stacktrace: ${exception.stacktrace}")
                .setBody(constant(STATUS_EXPORT_FAILED))
                .to("direct:notifyMarduk")
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
                .process(exchange -> {
                    InputStream timetableDataset = exchange.getIn().getHeader(TIMETABLE_DATASET_FILE, InputStream.class);
                    String codespace = exchange.getIn().getHeader(DATASET_CODESPACE, String.class).replace("rb_", "").toUpperCase();
                    GtfsExporter gtfsExporter = new EnturGtfsExporter(codespace, stopAreaRepositoryFactory.getStopAreaRepository());
                    exchange.getIn().setBody(gtfsExporter.convertNetexToGtfs(timetableDataset));
                })
                .log(LoggingLevel.INFO, correlation() + "Dataset processing complete")
                .routeId("convert-to-gtfs");

        from("direct:uploadGtfsDataset")
                .setHeader(FILE_HANDLE, simple(gtfsExportFilePath))
                .log(LoggingLevel.INFO, correlation() + "Uploading GTFS file " + GTFS_EXPORT_FILE_NAME + " to GCS file ${header." + FILE_HANDLE + "}")
                .to("direct:uploadMardukBlob")
                .log(LoggingLevel.INFO, correlation() + "Uploaded GTFS file " + GTFS_EXPORT_FILE_NAME + " to GCS file ${header." + FILE_HANDLE + "}")
                .routeId("upload-gtfs-dataset");

        from("direct:notifyMarduk")
                .to("google-pubsub:{{damu.pubsub.project.id}}:DamuExportGtfsStatusQueue")
                .routeId("notify-marduk");

    }
}
