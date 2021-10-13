package no.entur.damu.export.util;

import no.entur.damu.export.repository.GtfsDatasetRepository;
import no.entur.damu.export.repository.NetexDatasetRepository;
import org.onebusaway.gtfs.model.Stop;

public final class StopUtil {

    private StopUtil() {
    }

    public static Stop getGtfsStopFromScheduledStopPointId(String scheduledStopPointId, NetexDatasetRepository netexDatasetRepository, GtfsDatasetRepository gtfsDatasetRepository) {
        String quayId = netexDatasetRepository.getQuayIdByScheduledStopPointId(scheduledStopPointId);
        return gtfsDatasetRepository.getStopById(quayId);
    }
}
