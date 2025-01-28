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

package no.entur.damu.routes.validation;

import static no.entur.damu.Constants.DATASET_REFERENTIAL;
import static no.entur.damu.Constants.FILE_HANDLE;

import java.io.InputStream;
import no.entur.damu.Constants;
import no.entur.damu.gtfs.GtfsValidator;
import no.entur.damu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

@Component
public class GtfsValidationQueueRouteBuilder extends BaseRouteBuilder {

  private static final String GTFS_VALIDATION_REPORT_FILE_NAME =
    Constants.GTFS_VALIDATION_REPORTS_FILENAME_PREFIX +
    "${header." +
    DATASET_REFERENTIAL +
    "}" +
    Constants.GTFS_VALIDATION_REPORTS_FILENAME_SUFFIX;

  private final String gtfsValidationReportsFilePath;

  public GtfsValidationQueueRouteBuilder() {
    super();
    this.gtfsValidationReportsFilePath = GTFS_VALIDATION_REPORT_FILE_NAME;
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    from("direct:validateGtfs")
      .to("direct:validateGtfsDataset")
      .to("direct:uploadValidationReports")
      .routeId("validate-gtfs");

    from("direct:validateGtfsDataset")
      .log(LoggingLevel.INFO, "Validating GTFS dataset")
      .process(exchange -> {
        exchange
          .getIn()
          .setBody(
            GtfsValidator.validate(
              exchange.getIn().getHeader(DATASET_REFERENTIAL, String.class),
              exchange.getIn().getBody(InputStream.class)
            )
          );
      })
      .log(LoggingLevel.INFO, "GTFS validation complete")
      .routeId("validate-gtfs-dataset");

    from("direct:uploadValidationReports")
      .setHeader(FILE_HANDLE, simple(gtfsValidationReportsFilePath))
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Uploading GTFS validation reports " +
        GTFS_VALIDATION_REPORT_FILE_NAME +
        " to GCS file ${header." +
        FILE_HANDLE +
        "}"
      )
      .to("direct:uploadDamuBlob")
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Uploaded GTFS validation reports " +
        GTFS_VALIDATION_REPORT_FILE_NAME +
        " to GCS file ${header." +
        FILE_HANDLE +
        "}"
      )
      .routeId("upload-gtfs-validation-reports");
  }
}
