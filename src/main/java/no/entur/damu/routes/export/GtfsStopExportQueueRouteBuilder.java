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

import no.entur.damu.netex.EnturGtfsExporter;
import no.entur.damu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.entur.netex.gtfs.export.GtfsExporter;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.springframework.stereotype.Component;

import static no.entur.damu.Constants.BLOBSTORE_MAKE_BLOB_PUBLIC;
import static no.entur.damu.Constants.FILE_HANDLE;

/**
 * Generate a GTFS stops export and upload it to the GCS bucket.
 */
@Component
public class GtfsStopExportQueueRouteBuilder extends BaseRouteBuilder {

    private static final String GTFS_EXPORT_FILE_NAME = "tiamat/Full_latest-gtfs.zip";

    private final StopAreaRepositoryFactory stopAreaRepositoryFactory;

    public GtfsStopExportQueueRouteBuilder(StopAreaRepositoryFactory stopAreaRepositoryFactory) {
        super();
        this.stopAreaRepositoryFactory = stopAreaRepositoryFactory;
    }

    @Override
    public void configure() throws Exception {
        super.configure();

        from("direct:exportStops")
                .to("direct:convertStopsToGtfs")
                .to("direct:uploadStopsGtfsDataset")
                .routeId("export-stops");

        from("direct:convertStopsToGtfs")
                .log(LoggingLevel.INFO, correlation() + "Converting Stops to GTFS")
                .process(exchange -> {
                    GtfsExporter gtfsExporter = new EnturGtfsExporter(stopAreaRepositoryFactory.getStopAreaRepository());
                    exchange.getIn().setBody(gtfsExporter.convertStopsToGtfs());
                })
                .log(LoggingLevel.INFO, correlation() + "Converted Stops to GTFS")
                .routeId("export-stops-convert-to-gtfs");

        from("direct:uploadStopsGtfsDataset")
                .setHeader(FILE_HANDLE, simple(GTFS_EXPORT_FILE_NAME))
                .setHeader(BLOBSTORE_MAKE_BLOB_PUBLIC, simple("true", Boolean.class))
                .log(LoggingLevel.INFO, correlation() + "Uploading GTFS file " + GTFS_EXPORT_FILE_NAME + " to GCS file ${header." + FILE_HANDLE + "}")
                .to("direct:uploadMardukBlob")
                .log(LoggingLevel.INFO, correlation() + "Uploaded GTFS file " + GTFS_EXPORT_FILE_NAME + " to GCS file ${header." + FILE_HANDLE + "}")
                .routeId("export-stops-upload-gtfs-dataset");

    }
}
