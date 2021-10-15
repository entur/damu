package no.entur.damu.export.util;

import no.entur.damu.export.repository.GtfsDatasetRepository;
import no.entur.damu.export.repository.NetexDatasetRepository;
import org.onebusaway.gtfs.model.Stop;

/**
 * Utility for GTFS Stop.
 */
public final class StopUtil {

    private StopUtil() {
    }

    /**
     * Return the GTFS stop corresponding to a given Scheduled Stop Point.
     * @param scheduledStopPointId the Scheduled Stop Point.
     * @param netexDatasetRepository the NeTEx dataset repository.
     * @param gtfsDatasetRepository the GTFS dataset repository.
     * @return the GTFS stop corresponding to the Scheduled Stop Point.
     */
    public static Stop getGtfsStopFromScheduledStopPointId(String scheduledStopPointId, NetexDatasetRepository netexDatasetRepository, GtfsDatasetRepository gtfsDatasetRepository) {
        String quayId = netexDatasetRepository.getQuayIdByScheduledStopPointId(scheduledStopPointId);
        return gtfsDatasetRepository.getStopById(quayId);
    }
}
