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

package no.entur.damu.routes.stop;

import no.entur.damu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.entur.damu.Constants.FILE_HANDLE;

/**
 * Refresh the stop area repository.
 */
@Component
public class StopAreaRepositoryRouteBuilder extends BaseRouteBuilder {

    private final String stopExportFilename;
    private final String quartzTrigger;

    public StopAreaRepositoryRouteBuilder(@Value("${damu.netex.stop.filename:tiamat/Full_latest.zip}") String stopExportFilename,
                                          @Value("${damu.netex.stop.cache.refresh.quartz.trigger:?cron=0+0+03+?+*+*}") String quartzTrigger) {
        super();
        this.stopExportFilename = stopExportFilename;
        this.quartzTrigger = quartzTrigger;
    }

    @Override
    public void configure() throws Exception {
        super.configure();

        from("quartz://damu/refreshStopsAtStartup?" + "?fireNow=true&trigger.repeatCount=0")
                .to("direct:refreshStops")
                .routeId("stop-area-refresh-at-startup-quartz");

        from("quartz://damu/refreshStopsPeriodically?" + quartzTrigger)
                .to("direct:refreshStops")
                .routeId("stop-area-refresh-periodically-quartz");

        from("direct:refreshStops")
                .process(this::setNewCorrelationId)
                .log(LoggingLevel.DEBUG, correlation() + "Refreshing stop areas.")
                .to("direct:downloadNetexStopDataset")
                .bean("stopAreaRepositoryFactory", "refreshStopAreaRepository")
                .log(LoggingLevel.DEBUG, correlation() + "Refreshed stop areas.")
                .routeId("stop-area-refresh");

        from("direct:downloadNetexStopDataset")
                .log(LoggingLevel.INFO, correlation() + "Downloading NeTEx Stop dataset")
                .setHeader(FILE_HANDLE, constant(stopExportFilename))
                .to("direct:getMardukBlob")
                .filter(body().isNull())
                .log(LoggingLevel.ERROR, correlation() + "NeTEx Stopfile not found")
                .stop()
                //end filter
                .end()
                .routeId("download-netex-stop-dataset");
    }
}
