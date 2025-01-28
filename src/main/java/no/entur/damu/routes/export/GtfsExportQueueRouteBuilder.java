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

package no.entur.damu.routes.export;

import static no.entur.damu.Constants.BLOBSTORE_PATH_OUTBOUND;
import static no.entur.damu.Constants.DATASET_REFERENTIAL;
import static no.entur.damu.Constants.FILE_HANDLE;

import java.io.InputStream;
import no.entur.damu.Constants;
import no.entur.damu.netex.EnturGtfsExporter;
import no.entur.damu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.entur.netex.gtfs.export.GtfsExporter;
import org.entur.netex.gtfs.export.exception.GtfsExportException;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Receive a notification when a new NeTEx export is available in the blob store and convert it into a GTFS dataset.
 */
@Component
public class GtfsExportQueueRouteBuilder extends BaseRouteBuilder {

  private static final String TIMETABLE_EXPORT_FILE_NAME =
    BLOBSTORE_PATH_OUTBOUND +
    Constants.NETEX_FILENAME_PREFIX +
    "${header." +
    DATASET_REFERENTIAL +
    "}" +
    Constants.NETEX_FILENAME_SUFFIX;
  private static final String GTFS_EXPORT_FILE_NAME =
    Constants.GTFS_FILENAME_PREFIX +
    "${header." +
    DATASET_REFERENTIAL +
    "}" +
    Constants.GTFS_FILENAME_SUFFIX;

  static final String TIMETABLE_DATASET_FILE = "TIMETABLE_DATASET_FILE";

  private static final String STATUS_EXPORT_STARTED = "started";
  private static final String STATUS_EXPORT_OK = "ok";
  private static final String STATUS_EXPORT_FAILED = "failed";
  private final StopAreaRepositoryFactory stopAreaRepositoryFactory;
  private final String gtfsExportFilePath;
  private final boolean generateStaySeatedTransfer;

  public GtfsExportQueueRouteBuilder(
    StopAreaRepositoryFactory stopAreaRepositoryFactory,
    @Value("${damu.gtfs.export.folder:damu}") String gtfsExportFolder,
    @Value(
      "${damu.gtfs.export.transfer.stayseated:false}"
    ) boolean generateStaySeatedTransfer
  ) {
    super();
    this.stopAreaRepositoryFactory = stopAreaRepositoryFactory;
    this.gtfsExportFilePath = gtfsExportFolder + '/' + GTFS_EXPORT_FILE_NAME;
    this.generateStaySeatedTransfer = generateStaySeatedTransfer;
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    onException(GtfsExportException.class)
      .log(
        LoggingLevel.ERROR,
        correlation() +
        "Dataset processing failed: ${exception.message} stacktrace: ${exception.stacktrace}"
      )
      .setBody(constant(STATUS_EXPORT_FAILED))
      .to("direct:notifyMarduk")
      .end();

    from(
      "google-pubsub:{{damu.pubsub.project.id}}:DamuExportGtfsQueue?synchronousPull=true"
    )
      .process(this::setCorrelationIdIfMissing)
      .setHeader(DATASET_REFERENTIAL, bodyAs(String.class))
      .log(LoggingLevel.INFO, correlation() + "Received GTFS export request")
      .setBody(constant(STATUS_EXPORT_STARTED))
      .to("direct:notifyMarduk")
      .to("direct:downloadNetexTimetableDataset")
      .log(LoggingLevel.INFO, correlation() + "NeTEx Timetable file downloaded")
      .setHeader(TIMETABLE_DATASET_FILE, body())
      .process(this::extendAckDeadline)
      .to("direct:convertToGtfs")
      .process(this::extendAckDeadline)
      .multicast()
      .parallelProcessing()
      .to("direct:validateGtfs", "direct:uploadGtfsDataset")
      .end()
      .process(this::extendAckDeadline)
      .setBody(constant(STATUS_EXPORT_OK))
      .to("direct:notifyMarduk")
      .routeId("gtfs-export-queue");

    from("direct:downloadNetexTimetableDataset")
      .log(
        LoggingLevel.INFO,
        correlation() + "Downloading NeTEx Timetable dataset"
      )
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
        InputStream timetableDataset = exchange
          .getIn()
          .getHeader(TIMETABLE_DATASET_FILE, InputStream.class);
        String codespace = exchange
          .getIn()
          .getHeader(DATASET_REFERENTIAL, String.class)
          .replace("rb_", "")
          .toUpperCase();

        GtfsExporter gtfsExporter = new EnturGtfsExporter(
          codespace,
          stopAreaRepositoryFactory.getStopAreaRepository(),
          generateStaySeatedTransfer
        );
        exchange
          .getIn()
          .setBody(gtfsExporter.convertTimetablesToGtfs(timetableDataset));
      })
      .log(LoggingLevel.INFO, correlation() + "Dataset processing complete")
      .routeId("convert-to-gtfs");

    from("direct:uploadGtfsDataset")
      .setHeader(FILE_HANDLE, simple(gtfsExportFilePath))
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Uploading GTFS file " +
        GTFS_EXPORT_FILE_NAME +
        " to GCS file ${header." +
        FILE_HANDLE +
        "}"
      )
      .to("direct:uploadMardukBlob")
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Uploaded GTFS file " +
        GTFS_EXPORT_FILE_NAME +
        " to GCS file ${header." +
        FILE_HANDLE +
        "}"
      )
      .routeId("upload-gtfs-dataset");

    from("direct:notifyMarduk")
      .to("google-pubsub:{{damu.pubsub.project.id}}:DamuExportGtfsStatusQueue")
      .routeId("notify-marduk");
  }
}
