package no.entur.damu.gtfsconverter;

import org.entur.netex.gtfs.export.exception.StopPlaceNotFoundException;
import org.rutebanken.netex.model.StopPlace;

public class StopPlaceFetcher extends EnturNetexEntityFetcher<StopPlace, String> {

    @Override
    public StopPlace tryFetch(String quayId) {
        try {
            return this.webClient.get()
                    .uri("/quays/{quayId}/stop-place", quayId)
                    .retrieve()
                    .bodyToMono(StopPlace.class)
                    .block();
        } catch (Exception e) {
            throw new StopPlaceNotFoundException("Could not find StopPlace for quay id " + quayId + " due to " + e.getMessage());
        }
    }
}
