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
import org.entur.netex.gtfs.export.loader.NetexDatasetLoader;
import org.entur.netex.gtfs.export.producer.FeedInfoProducer;
import org.entur.netex.gtfs.export.repository.NetexDatasetRepository;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Custom GTFS exporter that handles missing or incomplete data in the input NeTEx dataset.
 */
public class EnturGtfsExporter extends DefaultGtfsExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnturGtfsExporter.class);
    private final NetexDatasetLoader netexDatasetLoader;

    public EnturGtfsExporter(StopAreaRepositoryFactory stopAreaRepositoryFactory,
                             NetexDatasetLoader netexDatasetLoader,
                             FeedInfoProducer feedInfoProducer) {
        super(stopAreaRepositoryFactory, feedInfoProducer);
        this.netexDatasetLoader = netexDatasetLoader;
    }

    @Override
    protected void loadNetexTimetableDatasetToRepository(InputStream netexTimetableDataset,
                                                         NetexDatasetRepository netexDatasetRepository) {
        LOGGER.info("Importing NeTEx Timetable dataset");
        netexDatasetLoader.load(netexTimetableDataset, netexDatasetRepository);
        LOGGER.info("Imported NeTEx Timetable dataset");
    }
}
