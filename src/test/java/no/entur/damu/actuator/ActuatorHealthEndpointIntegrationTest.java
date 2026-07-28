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

package no.entur.damu.actuator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import no.entur.damu.TestApp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.gcloud.PubSubEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Verifies that the actuator endpoints exposed in production are reachable without
 * authentication, so that the Kubernetes probes and the Prometheus scrape keep working.
 */
@SpringBootTest(
  classes = TestApp.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  // mirrors the actuator configuration in helm/damu/templates/configmap.yaml
  properties = {
    "management.endpoints.access.default=none",
    "management.endpoint.info.access=unrestricted",
    "management.endpoint.health.access=unrestricted",
    "management.endpoint.health.group.readiness.include=readinessState",
    "management.endpoint.prometheus.access=unrestricted",
    "management.endpoints.web.exposure.include=info,health,prometheus",
    "management.endpoints.web.exposure.exclude=",
  }
)
@AutoConfigureTestRestTemplate
@ActiveProfiles(
  { "test", "default", "in-memory-blobstore", "google-pubsub-autocreate" }
)
@Testcontainers
class ActuatorHealthEndpointIntegrationTest {

  @Container
  private static final PubSubEmulatorContainer pubsubEmulator =
    new PubSubEmulatorContainer(
      DockerImageName.parse(
        "gcr.io/google.com/cloudsdktool/cloud-sdk:emulators"
      )
    );

  @DynamicPropertySource
  static void emulatorProperties(DynamicPropertyRegistry registry) {
    registry.add(
      "spring.cloud.gcp.pubsub.emulator-host",
      pubsubEmulator::getEmulatorEndpoint
    );
    registry.add(
      "camel.component.google-pubsub.endpoint",
      pubsubEmulator::getEmulatorEndpoint
    );
  }

  @Autowired
  private TestRestTemplate restTemplate;

  @ParameterizedTest
  @ValueSource(
    strings = {
      "/actuator/info",
      "/actuator/health",
      "/actuator/health/liveness",
      "/actuator/health/readiness",
      "/actuator/prometheus",
    }
  )
  void testExposedEndpointIsAccessible(String path) {
    ResponseEntity<String> response = restTemplate.getForEntity(
      path,
      String.class
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @ParameterizedTest
  @ValueSource(
    strings = {
      "/actuator/health",
      "/actuator/health/liveness",
      "/actuator/health/readiness",
    }
  )
  void testHealthEndpointReturnsUpStatus(String path) {
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
      path,
      HttpMethod.GET,
      null,
      new ParameterizedTypeReference<>() {}
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertEquals("UP", body.get("status"));
  }

  @Test
  void testPrometheusEndpointReturnsMetrics() {
    ResponseEntity<String> response = restTemplate.getForEntity(
      "/actuator/prometheus",
      String.class
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    String body = response.getBody();
    assertNotNull(body);
    assertTrue(body.contains("# HELP"), body);
  }
}
