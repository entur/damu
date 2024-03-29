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

import static no.entur.damu.Constants.FILE_HANDLE;

import no.entur.damu.netex.EnturGtfsExporter;
import no.entur.damu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.entur.netex.gtfs.export.GtfsExporter;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generate a GTFS stops export and upload it to the GCS bucket.
 */
@Component
public class GtfsStopExportQueueRouteBuilder extends BaseRouteBuilder {

  static final String GTFS_STOP_EXPORT_FILE_NAME =
    "tiamat/Current_latest-gtfs.zip";

  private final StopAreaRepositoryFactory stopAreaRepositoryFactory;
  private final String stopExportFilename;
  private final String quartzTrigger;

  public GtfsStopExportQueueRouteBuilder(
    StopAreaRepositoryFactory stopAreaRepositoryFactory,
    @Value(
      "${damu.netex.stop.current.filename:tiamat/Current_latest.zip}"
    ) String stopExportFilename,
    @Value(
      "${damu.netex.stop.export.quartz.trigger:?cron=0+30+03+?+*+*}"
    ) String quartzTrigger
  ) {
    super();
    this.stopAreaRepositoryFactory = stopAreaRepositoryFactory;
    this.stopExportFilename = stopExportFilename;
    this.quartzTrigger = quartzTrigger;
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    from("quartz://damu/exportStopsPeriodically?" + quartzTrigger)
      .to("direct:exportStops")
      .routeId("export-stops-quartz");

    from("direct:exportStops")
      .to("direct:downloadCurrentStopsNetexDataset")
      .to("direct:convertCurrentStopsToGtfs")
      .to("direct:uploadCurrentStopsGtfsDataset")
      .routeId("export-stops");

    from("direct:downloadCurrentStopsNetexDataset")
      .log(
        LoggingLevel.INFO,
        correlation() + "Downloading Current Stop dataset"
      )
      .setHeader(FILE_HANDLE, constant(stopExportFilename))
      .to("direct:getMardukBlob")
      .filter(body().isNull())
      .log(LoggingLevel.ERROR, correlation() + "NeTEx Stopfile not found")
      .stop()
      //end filter
      .end()
      .routeId("download-current-stop-netex-dataset");

    from("direct:uploadCurrentStopsGtfsDataset")
      .setHeader(FILE_HANDLE, simple(GTFS_STOP_EXPORT_FILE_NAME))
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Uploading GTFS file " +
        GTFS_STOP_EXPORT_FILE_NAME +
        " to GCS file ${header." +
        FILE_HANDLE +
        "}"
      )
      .to("direct:uploadMardukBlob")
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Uploaded GTFS file " +
        GTFS_STOP_EXPORT_FILE_NAME +
        " to GCS file ${header." +
        FILE_HANDLE +
        "}"
      )
      .routeId("upload-current-stops-gtfs-dataset");

    from("direct:convertCurrentStopsToGtfs")
      .log(
        LoggingLevel.INFO,
        correlation() + "Converting Current Stops to GTFS"
      )
      .process(exchange -> {
        GtfsExporter gtfsExporter = new EnturGtfsExporter(
          stopAreaRepositoryFactory.getStopAreaRepository()
        );
        exchange.getIn().setBody(gtfsExporter.convertStopsToGtfs());
      })
      .log(LoggingLevel.INFO, correlation() + "Converted Current Stops to GTFS")
      .routeId("convert-current-stops-to-gtfs");
  }
}
