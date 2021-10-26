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

package no.entur.damu.routes;

import no.entur.damu.Constants;
import no.entur.damu.DamuRouteBuilderIntegrationTestBase;
import no.entur.damu.TestApp;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;

import static no.entur.damu.Constants.BLOBSTORE_PATH_OUTBOUND;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestApp.class)
class GtfsExportQueueRouteBuilderTest extends DamuRouteBuilderIntegrationTestBase {

    private static final String CODESPACE = "rb_flb";

    @Produce("google-pubsub:{{damu.pubsub.project.id}}:DamuExportGtfsQueue")
    protected ProducerTemplate gtfsExportQueueProducerTemplate;

    @EndpointInject("mock:checkUploadedDataset")
    protected MockEndpoint checkUploadedDataset;

    @Value("${damu.netex.stop.filename:tiamat/Full_latest.zip}")
    private String stopExportFilename;

    @Test
    void testExportGtfs() throws Exception {

        AdviceWith.adviceWith(context, "upload-gtfs-dataset", a -> a.weaveAddLast().to("mock:checkUploadedDataset"));
        checkUploadedDataset.expectedMessageCount(1);
        checkUploadedDataset.setResultWaitTime(200000);

        mardukInMemoryBlobStoreRepository.uploadBlob(BLOBSTORE_PATH_OUTBOUND + Constants.NETEX_FILENAME_PREFIX + CODESPACE + Constants.NETEX_FILENAME_SUFFIX,
                getClass().getResourceAsStream("/rb_flb-aggregated-netex.zip"), true);

        mardukInMemoryBlobStoreRepository.uploadBlob(stopExportFilename,
                getClass().getResourceAsStream("/RailStations_latest.zip"), true);

        context.start();
        gtfsExportQueueProducerTemplate.sendBody(CODESPACE);
        checkUploadedDataset.assertIsSatisfied();

        InputStream gtfsExport = mardukInMemoryBlobStoreRepository.getBlob("damu/" + Constants.GTFS_FILENAME_PREFIX + CODESPACE + Constants.GTFS_FILENAME_SUFFIX);
        Assertions.assertNotNull(gtfsExport);
        byte[] content = gtfsExport.readAllBytes();
        Assertions.assertTrue(content.length > 0);
    }


}
