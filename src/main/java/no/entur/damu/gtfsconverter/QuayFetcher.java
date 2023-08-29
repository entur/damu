package no.entur.damu.gtfsconverter;

import org.entur.netex.gtfs.export.exception.QuayNotFoundException;
import org.rutebanken.netex.model.Quay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QuayFetcher extends EnturNetexEntityFetcher<Quay, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuayFetcher.class);

    public QuayFetcher(
            @Value("${stopplace.registry.url:https://api.dev.entur.io/stop-places/v1/read}")
            String stopPlaceRegistryUrl,
            @Value("${http.client.name:damu}")
            String clientName,
            @Value("${http.client.id:damu}")
            String clientId) {
        super(stopPlaceRegistryUrl, clientId, clientName);
    }

    @Override
    public Quay tryFetch(String quayId) {
        LOGGER.info("Trying to fetch the Quay with id {}, from read API", quayId);
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
