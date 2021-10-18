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

package no.entur.damu.export.producer;

import no.entur.damu.export.model.GtfsRouteType;
import no.entur.damu.export.repository.GtfsDatasetRepository;
import no.entur.damu.export.repository.NetexDatasetRepository;
import no.entur.damu.export.repository.TestGtfsRepository;
import no.entur.damu.export.repository.TestNetexDatasetRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.Route;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.GroupOfLinesRefStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.MultilingualString;

class RouteProducerTest {

    private static final String LINE_ID = "Line-ID";
    private static final String LINE_NAME = "Line-Name";


    @Test
    void testRouteProducer() {

        NetexDatasetRepository netexDatasetRepository = new TestNetexDatasetRepository();
        GtfsDatasetRepository gtfsDatasetRepository = new TestGtfsRepository();

        RouteProducer routeProducer = new DefaultRouteProducer(netexDatasetRepository, gtfsDatasetRepository);
        Line line = new Line();
        line.setId(LINE_ID);
        MultilingualString lineName = new MultilingualString();
        lineName.setValue(LINE_NAME);
        line.setName(lineName);
        line.setTransportMode(AllVehicleModesOfTransportEnumeration.BUS);
        GroupOfLinesRefStructure groupOfLineRef = new GroupOfLinesRefStructure();
        groupOfLineRef.setRef(TestNetexDatasetRepository.NETWORK_ID);
        line.setRepresentedByGroupRef(groupOfLineRef);

        Route route = routeProducer.produce(line);

        Assertions.assertNotNull(route);
        Assertions.assertNotNull(route.getId());
        Assertions.assertEquals(LINE_ID, route.getId().getId());
        Assertions.assertEquals(LINE_NAME, route.getLongName());
        Assertions.assertEquals(GtfsRouteType.BusService.getValue(), route.getType());

    }
}