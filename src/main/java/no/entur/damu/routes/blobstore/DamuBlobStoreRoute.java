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

package no.entur.damu.routes.blobstore;

import static no.entur.damu.Constants.FILE_HANDLE;

import no.entur.damu.routes.BaseRouteBuilder;
import no.entur.damu.services.DamuBlobStoreService;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

@Component
public class DamuBlobStoreRoute extends BaseRouteBuilder {

  private final DamuBlobStoreService damuBlobStoreService;

  public DamuBlobStoreRoute(DamuBlobStoreService damuBlobStoreService) {
    this.damuBlobStoreService = damuBlobStoreService;
  }

  @Override
  public void configure() {
    from("direct:uploadDamuBlob")
      .to(logDebugShowAll())
      .bean(damuBlobStoreService, "uploadBlob")
      .setBody(simple(""))
      .to(logDebugShowAll())
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Stored file ${header." +
        FILE_HANDLE +
        "} in Damu bucket."
      )
      .routeId("blobstore-damu-upload");
  }
}
