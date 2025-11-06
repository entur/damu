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

import no.entur.damu.TestApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The main purpose of this
 */
@SpringBootTest(
  classes = TestApp.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
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
    registry.add("management.endpoints.web.exposure.include", () -> "health");
    registry.add("management.endpoints.web.exposure.exclude", () -> "");
    registry.add("management.endpoint.health.enabled", () -> "true");
    registry.add("management.endpoint.health.show-details", () -> "always");
  }

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void testHealthEndpointIsAccessible() {
    ResponseEntity<String> response = restTemplate.getForEntity(
      "/actuator/health",
      String.class
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void testHealthEndpointReturnsUpStatus() {
    ResponseEntity<String> response = restTemplate.getForEntity(
      "/actuator/health",
      String.class
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    String body = response.getBody();
    assertNotNull(body);
    assertTrue(body.contains("\"status\":\"UP\""));
  }
}
