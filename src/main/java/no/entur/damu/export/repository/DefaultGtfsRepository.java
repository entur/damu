package no.entur.damu.export.repository;

import no.entur.damu.export.serializer.DefaultGtfsSerializer;
import no.entur.damu.export.serializer.GtfsSerializer;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsMutableDao;

import java.io.InputStream;

public class DefaultGtfsRepository implements GtfsDatasetRepository {

    private static final String ENTUR_AGENCY_ID = "XXX";

    private final GtfsMutableDao gtfsDao;
    private final GtfsSerializer gtfsSerializer;
    private final Agency defaultAgency;

    public DefaultGtfsRepository() {
        this.gtfsDao = new GtfsRelationalDaoImpl();
        this.gtfsSerializer = new DefaultGtfsSerializer(gtfsDao);
        this.defaultAgency = createDefaultAgency();
    }

    @Override
    public Agency getAgencyById(String agencyId) {
        return gtfsDao.getAgencyForId(agencyId);
    }

    @Override
    public Trip getTripById(String tripId) {
        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setId(tripId);
        agencyAndId.setAgencyId(defaultAgency.getId());

        return gtfsDao.getTripForId(agencyAndId);
    }

    @Override
    public Stop getStopById(String stopId) {
        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setId(stopId);
        agencyAndId.setAgencyId(defaultAgency.getId());
        return gtfsDao.getStopForId(agencyAndId);
    }

    @Override
    public void saveEntity(Object entity) {
        gtfsDao.saveEntity(entity);
    }

    @Override
    public InputStream writeGtfs() {
        return gtfsSerializer.writeGtfs();
    }

    @Override
    public Agency getDefaultAgency() {
        return defaultAgency;
    }

    /**
     * Return a default agency.
     * The OneBusAway API requires an agency linked to stops, even if it does not appear in the GTFS export
     *
     * @return a default agency.
     */
    private static Agency createDefaultAgency() {
        Agency enturAgency = new Agency();
        enturAgency.setId(ENTUR_AGENCY_ID);
        enturAgency.setUrl("https://notinuse");
        enturAgency.setName("Default agency");
        return enturAgency;
    }

}
