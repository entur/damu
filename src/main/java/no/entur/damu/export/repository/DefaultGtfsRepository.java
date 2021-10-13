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

    private static final String ENTUR_AGENCY_ID = "ENT";

    private final GtfsMutableDao gtfsDao;
    private final GtfsSerializer gtfsSerializer;
    private final Agency defaultAgency;

    public DefaultGtfsRepository() {
        this.gtfsDao = new GtfsRelationalDaoImpl();
        this.gtfsSerializer = new DefaultGtfsSerializer(gtfsDao);
        this.defaultAgency = createDefaultAgency();
    }

    @Override
    public Agency getAgencyById(String authorityId) {
        return gtfsDao.getAgencyForId(authorityId);
    }

    @Override
    public void saveEntity(Object entity) {
        gtfsDao.saveEntity(entity);
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
    public InputStream writeGtfs() {
        return gtfsSerializer.writeGtfs();
    }

    @Override
    public Agency getDefaultAgency() {
        return defaultAgency;
    }

    /**
     * Return an agency representing Entur.
     * The OneBusAway API requires an agency linked to stops, even if it does not appear in the GTFS export
     *
     * @return an agency representing Entur.
     */
    private Agency createDefaultAgency() {
        Agency enturAgency = new Agency();
        enturAgency.setId(ENTUR_AGENCY_ID);
        enturAgency.setUrl("https://www.entur.org");
        enturAgency.setName("Entur");
        return enturAgency;
    }

}
