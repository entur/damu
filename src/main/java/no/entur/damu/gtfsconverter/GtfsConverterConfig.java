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

package no.entur.damu.gtfsconverter;

import no.entur.damu.netex.EnturFeedInfoProducer;
import no.entur.damu.netex.EnturGtfsExporter;
import no.entur.damu.netex.EnturNetexDatasetLoader;
import org.entur.netex.gtfs.export.loader.NetexDatasetLoader;
import org.entur.netex.gtfs.export.producer.FeedInfoProducer;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GtfsConverterConfig {

    @Bean
    public StopAreaRepositoryFactory stopAreaRepositoryFactory(QuayFetcher quayFetcher,
                                                               StopPlaceFetcher stopPlaceFetcher) {
        return new EnturStopAreaRepositoryFactory(quayFetcher, stopPlaceFetcher);
    }

    @Bean
    public NetexDatasetLoader netexDatasetLoader() {
        return new EnturNetexDatasetLoader();
    }

    @Bean
    public FeedInfoProducer feedInfoProducer() {
        return new EnturFeedInfoProducer();
    }

    @Bean
    public EnturGtfsExporter gtfsExporter(StopAreaRepositoryFactory stopAreaRepositoryFactory,
                                          NetexDatasetLoader netexDatasetLoader,
                                          FeedInfoProducer feedInfoProducer) {
        return new EnturGtfsExporter(stopAreaRepositoryFactory, netexDatasetLoader, feedInfoProducer);
    }
}