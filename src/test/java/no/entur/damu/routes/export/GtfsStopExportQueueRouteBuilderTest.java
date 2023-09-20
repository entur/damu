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

import no.entur.damu.DamuRouteBuilderIntegrationTestBase;
import no.entur.damu.TestApp;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static no.entur.damu.routes.export.GtfsStopExportQueueRouteBuilder.GTFS_STOP_EXPORT_FILE_NAME;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestApp.class)
class GtfsStopExportQueueRouteBuilderTest extends DamuRouteBuilderIntegrationTestBase {

    @Produce("direct:exportStops")
    protected ProducerTemplate directExportStops;


    @Value("${damu.netex.stop.current.filename:tiamat/Current_latest.zip}")
    private String stopExportFilename;

    @Autowired
    private StopAreaRepositoryFactory stopAreaRepositoryFactory;

    @Test
    void testGtfsStopExport() throws Exception {
        mardukInMemoryBlobStoreRepository.uploadBlob(stopExportFilename,
                getClass().getResourceAsStream("/Current_latest.zip"), true);

        // Populating the stopAreaRepository with stop areas.
        stopAreaRepositoryFactory.refreshStopAreaRepository(
                getClass().getResourceAsStream("/Current_latest.zip")
        );

        context.start();
        directExportStops.sendBody(null);

        InputStream gtfsExport = mardukInMemoryBlobStoreRepository.getBlob(GTFS_STOP_EXPORT_FILE_NAME);
        Assertions.assertNotNull(gtfsExport);

        String gtfsStops = extractGtfsStops(gtfsExport);
        Assertions.assertFalse(gtfsStops.isEmpty());
        String[] lines = gtfsStops.split("\n");
        Assertions.assertEquals(7, lines.length);
    }

    private static String extractGtfsStops(InputStream gtfsArchive) throws IOException {
        String gtfsStops = "";
        try (ZipInputStream zis = new ZipInputStream(gtfsArchive)) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                if (zipEntry.getName().equals("stops.txt")) {
                    gtfsStops = new String(zis.readAllBytes());
                }
                zipEntry = zis.getNextEntry();
            }
        }
        return gtfsStops;
    }


}
