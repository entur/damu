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

package no.entur.damu.netex;

import org.entur.netex.gtfs.export.DefaultGtfsExporter;
import org.entur.netex.gtfs.export.stop.StopAreaRepository;

/**
 * Custom GTFS exporter that handles missing or incomplete data in the input NeTEx dataset.
 */
public class EnturGtfsExporter extends DefaultGtfsExporter {

    public EnturGtfsExporter(String codespace, StopAreaRepository stopAreaRepository) {
        super(codespace, stopAreaRepository);
        setNetexDatasetLoader(new EnturNetexDatasetLoader());
        setAgencyProducer(new EnturAgencyProducer(getNetexDatasetRepository(), codespace));
        setFeedInfoProducer(new EnturFeedInfoProducer());
    }

    public EnturGtfsExporter(StopAreaRepository stopAreaRepository) {
        super(stopAreaRepository);
        setFeedInfoProducer(new EnturFeedInfoProducer());
    }
}
