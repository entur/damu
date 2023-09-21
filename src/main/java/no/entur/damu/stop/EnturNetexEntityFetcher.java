package no.entur.damu.stop;

import org.entur.netex.gtfs.export.stop.NetexEntityFetcher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Base class for fetchers that retrieve NeTEx entities from Entur stop place API.
 */
public abstract class EnturNetexEntityFetcher<R, S>
  implements NetexEntityFetcher<R, S> {

  protected static final String ET_CLIENT_ID_HEADER = "ET-Client-ID";
  protected static final String ET_CLIENT_NAME_HEADER = "ET-Client-Name";
  protected final WebClient webClient;

  protected EnturNetexEntityFetcher(
    String stopPlaceRegistryUrl,
    String clientId,
    String clientName
  ) {
    this.webClient =
      WebClient
        .builder()
        .baseUrl(stopPlaceRegistryUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
        .defaultHeader(ET_CLIENT_NAME_HEADER, clientName)
        .defaultHeader(ET_CLIENT_ID_HEADER, clientId)
        .build();
  }
}
