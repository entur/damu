package no.entur.damu.export.util;

import org.entur.netex.index.api.NetexEntitiesIndex;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.services.GtfsDao;

public final class StopUtil {

    private static final String ENTUR_AGENCY_ID = "ENT";

    private StopUtil() {
    }

    /**
     * Return an agency representing Entur.
     * The OneBusAway API requires an agency linked to stops, even if it does not appear in the GTFS export
     *
     * @return an agency representing Entur.
     */
    public static Agency createEnturAgency() {
        Agency enturAgency = new Agency();
        enturAgency.setId(ENTUR_AGENCY_ID);
        enturAgency.setUrl("https://www.entur.org");
        enturAgency.setName("Entur");
        return enturAgency;
    }

    public static Stop getGtfsStopFromScheduledStopPointId(String scheduledStopPointId, NetexEntitiesIndex netexEntitiesIndex, GtfsDao gtfsDao) {
        String quayId = netexEntitiesIndex.getQuayIdByStopPointRefIndex().get(scheduledStopPointId);
        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setId(quayId);
        agencyAndId.setAgencyId(ENTUR_AGENCY_ID);
        return gtfsDao.getStopForId(agencyAndId);
    }
}
