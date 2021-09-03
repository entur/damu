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

package no.entur.damu.routes.netex.notification;

import no.entur.damu.Constants;
import no.entur.damu.routes.BaseRouteBuilder;
import no.entur.damu.service.GtfsImport;
import org.apache.camel.LoggingLevel;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static no.entur.damu.Constants.BLOBSTORE_PATH_OUTBOUND;
import static no.entur.damu.Constants.DATASET_CODESPACE;
import static no.entur.damu.Constants.FILE_HANDLE;

/**
 * Receive a notification when a new NeTEx export is available in the blob store and convert it into a GTFS dataset.
 */
@Component
public class GtfsExportQueueRouteBuilder extends BaseRouteBuilder {

    private static final String TIMETABLE_EXPORT_FILE_NAME = BLOBSTORE_PATH_OUTBOUND + Constants.NETEX_FILENAME_PREFIX + "${body}" + Constants.NETEX_FILENAME_SUFFIX;
    private static final String STOP_EXPORT_FILE_NAME = "tiamat/Full_latest.zip";

    private static final String TIMETABLE_DATASET_FILE = "TIMETABLE_DATASET_FILE";
    private static final String STOP_DATASET_FILE = "STOP_DATASET_FILE";



    @Override
    public void configure() throws Exception {
        super.configure();


        from("master:lockOnDamuGtfsExportQueueRoute:google-pubsub:{{damu.pubsub.project.id}}:DamuGtfsExportQueue")

                .process(this::setCorrelationIdIfMissing)
                .setHeader(DATASET_CODESPACE, bodyAs(String.class))
                .log(LoggingLevel.INFO, correlation() + "Received GTFS export request")

                .to("direct:downloadNetexTimetableDataset")
                .filter(body().isNull())
                .log(LoggingLevel.ERROR, correlation() + "NeTEx Timetable file not found")
                .stop()
                //end filter
                .end()
                .log(LoggingLevel.INFO, correlation() + "NeTEx Timetable file downloaded")
                .setHeader(TIMETABLE_DATASET_FILE, body())

                .to("direct:downloadNetexStopDataset")
                .filter(body().isNull())
                .log(LoggingLevel.ERROR, correlation() + "NeTEx Stopfile not found")
                .stop()
                //end filter
                .end()
                .log(LoggingLevel.INFO, correlation() + "NeTEx Stop file downloaded")
                .setHeader(STOP_DATASET_FILE, body())


                .to("direct:convertToGtfs")
                .routeId("gtfs-export-queue");

        from("direct:downloadNetexTimetableDataset")
                .log(LoggingLevel.INFO, correlation() + "Downloading NeTEx Timetable dataset")
                .setHeader(FILE_HANDLE, simple(TIMETABLE_EXPORT_FILE_NAME))
                .to("direct:getMardukBlob")
                .routeId("download-netex-timetable-dataset");

        from("direct:downloadNetexStopDataset")
                .log(LoggingLevel.INFO, correlation() + "Downloading NeTEx Stop dataset")
                .setHeader(FILE_HANDLE, constant(STOP_EXPORT_FILE_NAME))
                .to("direct:getMardukBlob")
                .routeId("download-netex-stop-dataset");

        from("direct:convertToGtfs")
                .log(LoggingLevel.INFO, correlation() + "Converting to GTFS")
                .process(exchange -> {
                    InputStream timetableDataset = exchange.getIn().getHeader(TIMETABLE_DATASET_FILE, InputStream.class);
                    InputStream stopDataset = exchange.getIn().getHeader(TIMETABLE_DATASET_FILE, InputStream.class);
                    GtfsImport gtfsImport = new GtfsImport(timetableDataset, stopDataset);
                    gtfsImport.importNetex();
                    gtfsImport.convertNetexToGtfs();
                    InputStream exportedGtfs = gtfsImport.exportGtfs();

                    java.nio.file.Files.copy(
                            exportedGtfs,
                            Path.of("export-gtfs.zip"),
                            StandardCopyOption.REPLACE_EXISTING);

                    IOUtils.closeQuietly(exportedGtfs);

                })
                .log(LoggingLevel.INFO, correlation() + "Dataset processing complete")
                .routeId("notify-consumers-if-new");


    }

}
