package no.entur.damu.gtfsconverter;

import org.entur.netex.gtfs.export.exception.QuayNotFoundException;
import org.rutebanken.netex.model.Quay;

public class QuayFetcher extends EnturNetexEntityFetcher<Quay, String> {

    @Override
    public Quay tryFetch(String quayId) {
        try {
            return this.webClient.get()
                    .uri("/quays/{quayId}", quayId)
                    .retrieve()
                    .bodyToMono(Quay.class)
                    .block();
        } catch (Exception e) {
            throw new QuayNotFoundException("Could not find Quay for id " + quayId + " due to " + e.getMessage());
        }
    }
}
