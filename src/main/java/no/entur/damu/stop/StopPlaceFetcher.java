package no.entur.damu.stop;

import org.entur.netex.gtfs.export.exception.StopPlaceNotFoundException;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Retrieve stop places from Entur stop place API.
 */
@Component
public class StopPlaceFetcher
  extends EnturNetexEntityFetcher<StopPlace, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StopPlaceFetcher.class
  );

  public StopPlaceFetcher(
    @Value(
      "${stopplace.registry.url:https://api.dev.entur.io/stop-places/v1/read}"
    ) String stopPlaceRegistryUrl,
    @Value("${http.client.name:damu}") String clientName,
    @Value("${http.client.id:damu}") String clientId
  ) {
    super(stopPlaceRegistryUrl, clientId, clientName);
  }

  @Override
  public StopPlace tryFetch(String quayId) {
    LOGGER.info(
      "Trying to fetch the parent stop place of quay {}, from read API",
      quayId
    );

    try {
      return this.webClient.get()
        .uri("/quays/{quayId}/stop-place", quayId)
        .retrieve()
        .bodyToMono(StopPlace.class)
        .block();
    } catch (Exception e) {
      throw new StopPlaceNotFoundException(
        "Could not find StopPlace for quay id " +
        quayId +
        " due to " +
        e.getMessage()
      );
    }
  }
}
