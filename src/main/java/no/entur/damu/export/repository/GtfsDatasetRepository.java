package no.entur.damu.export.repository;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;

import java.io.InputStream;

public interface GtfsDatasetRepository {

    Agency getAgencyById(String agencyId);
    Trip getTripById(String tripId);
    Stop getStopById(String stopId);

    void saveEntity(Object entity);
    InputStream writeGtfs();

    Agency getDefaultAgency();
}
