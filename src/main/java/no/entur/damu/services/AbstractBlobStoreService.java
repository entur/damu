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

package no.entur.damu.services;

import java.io.InputStream;
import org.rutebanken.helper.storage.repository.BlobStoreRepository;

public abstract class AbstractBlobStoreService {

  protected final BlobStoreRepository repository;

  protected AbstractBlobStoreService(
    String containerName,
    BlobStoreRepository repository
  ) {
    this.repository = repository;
    this.repository.setContainerName(containerName);
  }

  public InputStream getBlob(String name) {
    return repository.getBlob(name);
  }

  /**
   * A metadata lookup. {@link #getBlob} downloads the whole object, and the GCS implementation reads it
   * twice: once to checksum it and once to hand it back.
   */
  public boolean exists(String name) {
    return repository.exist(name);
  }

  public void uploadBlob(String name, InputStream inputStream) {
    repository.uploadBlob(name, inputStream);
  }
}
