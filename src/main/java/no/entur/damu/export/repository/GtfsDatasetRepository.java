package no.entur.damu.export.repository;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;

import java.io.InputStream;

public interface GtfsDatasetRepository {
    Agency getAgencyById(String authorityId);

    void saveEntity(Object entity);

    Trip getTripById(String tripId);

    Stop getStopById(String quayId);

    InputStream writeGtfs();

    Agency getDefaultAgency();
}
