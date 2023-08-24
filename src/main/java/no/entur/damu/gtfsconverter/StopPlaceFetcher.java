package no.entur.damu.gtfsconverter;

import org.entur.netex.gtfs.export.exception.StopPlaceNotFoundException;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopPlaceFetcher extends EnturNetexEntityFetcher<StopPlace, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopPlaceFetcher.class);
    @Override
    public StopPlace tryFetch(String stopPlaceId) {

        LOGGER.info("Trying to fetch the stop place with id {}, from read API", stopPlaceId);

        try {
            return this.webClient.get()
                    .uri("/quays/{quayId}/stop-place", stopPlaceId)
                    .retrieve()
                    .bodyToMono(StopPlace.class)
                    .block();
        } catch (Exception e) {
            throw new StopPlaceNotFoundException("Could not find StopPlace for quay id " + stopPlaceId + " due to " + e.getMessage());
        }
    }
}
