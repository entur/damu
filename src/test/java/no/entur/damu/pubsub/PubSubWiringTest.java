package no.entur.damu.pubsub;

import static no.entur.damu.Constants.CORRELATION_ID;
import static no.entur.damu.Constants.GTFS_ROUTE_DISPATCHER_EXPORT_HEADER_VALUE;
import static no.entur.damu.Constants.GTFS_ROUTE_DISPATCHER_HEADER_NAME;
import static no.entur.damu.Constants.STATUS_EXPORT_STARTED;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PubsubMessage;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import no.entur.damu.TestApp;
import org.entur.pubsub.base.EnturGooglePubSubUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.gcloud.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Checks the transport, once: a request published by marduk is picked up and dispatched, and the
 * status damu publishes carries the attributes marduk matches its pending job on.
 *
 * <p>The export request names a codespace with no NeTEx dataset behind it, so the job stops after the
 * STARTED notification. The jobs themselves are covered by the pipeline tests, which need no emulator.
 * This is the only test that pays for one.
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  classes = TestApp.class,
  properties = { "damu.pubsub.consumers.enabled=true" }
)
@ActiveProfiles({ "test", "default", "in-memory-blobstore" })
// Closed rather than left in the context cache: the PubSub admin clients hold non-daemon gax threads
// that otherwise live until the JVM exits.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PubSubWiringTest {

  private static final String CORRELATION = "wiring-test-correlation";

  private static PubSubEmulatorContainer pubsubEmulator;

  @Autowired
  private PubSubTemplate pubSubTemplate;

  private Subscriber statusSubscriber;

  @BeforeAll
  static void startEmulator() {
    pubsubEmulator =
      new PubSubEmulatorContainer(
        DockerImageName.parse(
          "gcr.io/google.com/cloudsdktool/cloud-sdk:emulators"
        )
      );
    pubsubEmulator.start();
  }

  @AfterAll
  static void stopEmulator() {
    pubsubEmulator.stop();
  }

  @DynamicPropertySource
  static void emulatorProperties(DynamicPropertyRegistry registry) {
    registry.add(
      "spring.cloud.gcp.pubsub.emulator-host",
      pubsubEmulator::getEmulatorEndpoint
    );
  }

  @AfterEach
  void closeStatusSubscriber() {
    if (statusSubscriber != null) {
      EnturGooglePubSubUtils.closeSubscriber(statusSubscriber);
      statusSubscriber = null;
    }
  }

  @Test
  void exportRequestIsConsumedAndTheStatusIsPublished() {
    List<PubsubMessage> statuses = new CopyOnWriteArrayList<>();
    statusSubscriber =
      pubSubTemplate.subscribe(
        DamuQueues.DAMU_EXPORT_GTFS_STATUS_QUEUE,
        message -> {
          statuses.add(message.getPubsubMessage());
          message.ack();
        }
      );

    pubSubTemplate.publish(
      DamuQueues.GTFS_ROUTE_DISPATCHER_TOPIC,
      "rb_nosuchcodespace",
      Map.of(
        GTFS_ROUTE_DISPATCHER_HEADER_NAME,
        GTFS_ROUTE_DISPATCHER_EXPORT_HEADER_VALUE,
        CORRELATION_ID,
        CORRELATION
      )
    );

    await().atMost(Duration.ofSeconds(30)).until(() -> !statuses.isEmpty());

    PubsubMessage status = statuses.getFirst();
    assertEquals(STATUS_EXPORT_STARTED, status.getData().toStringUtf8());
    assertEquals(
      CORRELATION,
      status.getAttributesMap().get(CORRELATION_ID),
      "marduk matches the status to its pending job on the correlation id"
    );
  }
}
