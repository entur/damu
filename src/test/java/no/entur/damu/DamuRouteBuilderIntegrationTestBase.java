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

package no.entur.damu;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import jakarta.annotation.PostConstruct;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.AfterEach;
import org.rutebanken.helper.storage.repository.BlobStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@CamelSpringBootTest
@UseAdviceWith
@ActiveProfiles(
  {
    "test",
    "default",
    "in-memory-blobstore",
    "google-pubsub-emulator",
    "google-pubsub-autocreate",
  }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class DamuRouteBuilderIntegrationTestBase {

  @Value("${blobstore.gcs.marduk.container.name}")
  private String mardukContainerName;

  @Value("${blobstore.gcs.damu.container.name}")
  private String damuContainerName;

  @Autowired
  protected ModelCamelContext context;

  @Autowired
  protected PubSubTemplate pubSubTemplate;

  @Autowired
  protected BlobStoreRepository mardukInMemoryBlobStoreRepository;

  @Autowired
  protected BlobStoreRepository damuInMemoryBlobStoreRepository;

  @EndpointInject("mock:sink")
  protected MockEndpoint sink;

  @PostConstruct
  void initInMemoryBlobStoreRepositories() {
    mardukInMemoryBlobStoreRepository.setContainerName(mardukContainerName);
    damuInMemoryBlobStoreRepository.setContainerName(damuContainerName);
  }

  @AfterEach
  void stopContext() {
    context.stop();
  }
}
