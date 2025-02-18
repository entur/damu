package no.entur.damu.routes.aggregation;

import no.entur.damu.DamuRouteBuilderIntegrationTestBase;
import no.entur.damu.TestApp;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = TestApp.class
)
public class GtfsAggregationQueueRouteBuilderTest extends DamuRouteBuilderIntegrationTestBase {
    @EndpointInject("mock:aggregateGtfsDone")
    private MockEndpoint aggregateGtfsDone;

    @Produce("google-pubsub:{{marduk.pubsub.project.id}}:DamuAggregateGtfsQueue")
    protected ProducerTemplate producerTemplate;

    @Test
    public void testRoute() throws Exception {

        mardukInMemoryBlobStoreRepository.uploadBlob(
                "outbound/gtfs/gtfs.zip",
                getClass().getResourceAsStream("/gtfs.zip")
        );
        mardukInMemoryBlobStoreRepository.uploadBlob(
                "outbound/gtfs/gtfs2.zip",
                getClass().getResourceAsStream("/gtfs2.zip")
        );

        AdviceWith.adviceWith(context, "aggregate-gtfs", a -> a.weaveAddLast().to("mock:aggregateGtfsDone"));
        aggregateGtfsDone.setExpectedMessageCount(1);
        context.start();

        Map<String, String> headers = new HashMap<>();
        sendBodyAndHeadersToPubSub(producerTemplate, "gtfs.zip,gtfs2.zip", headers);

        aggregateGtfsDone.assertIsSatisfied();
    }
}
