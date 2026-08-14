package no.entur.damu;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.rutebanken.helper.storage.repository.BlobStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs damu's jobs in one JVM against an in-memory blob store and a recording publisher.
 *
 * <p>No PubSub emulator: the consumer is what turns a message into a call on one of the services, and
 * that is the only thing {@code PubSubWiringTest} needs a broker for. Everything else calls the service
 * directly.
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  classes = TestApp.class
)
@Import(PipelineTestConfig.class)
@ActiveProfiles({ "test", "default", "in-memory-blobstore" })
public abstract class DamuPipelineTestBase {

  @Autowired
  protected RecordingPubSubPublisher publisher;

  @Autowired
  protected BlobStoreRepository mardukInMemoryBlobStoreRepository;

  @Autowired
  protected BlobStoreRepository damuInMemoryBlobStoreRepository;

  @Autowired
  private Map<String, Map<String, byte[]>> blobsInContainers;

  @Value("${blobstore.gcs.marduk.container.name}")
  private String mardukContainerName;

  @Value("${blobstore.gcs.damu.container.name}")
  private String damuContainerName;

  @PostConstruct
  void initInMemoryBlobStoreRepositories() {
    mardukInMemoryBlobStoreRepository.setContainerName(mardukContainerName);
    damuInMemoryBlobStoreRepository.setContainerName(damuContainerName);
  }

  /**
   * The context is shared by every test in the class, and so is the blob store behind both
   * repositories. Emptying it here is what keeps one test's uploads from satisfying another test.
   */
  @BeforeEach
  void resetRecordersAndBlobStore() {
    publisher.reset();
    blobsInContainers.clear();
  }

  protected static void assertNotEmptyBlob(
    BlobStoreRepository repository,
    String path
  ) throws IOException {
    InputStream blob = repository.getBlob(path);
    assertNotNull(blob, "Expected a blob at " + path);
    assertTrue(blob.readAllBytes().length > 0, path + " must be non-empty");
  }

  protected static InputStream testResource(String name) {
    InputStream resource =
      DamuPipelineTestBase.class.getResourceAsStream('/' + name);
    assertNotNull(resource, "Test resource not found: " + name);
    return resource;
  }
}
