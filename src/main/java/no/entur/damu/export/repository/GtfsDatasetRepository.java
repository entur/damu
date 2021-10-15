package no.entur.damu.export.repository;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;

import java.io.InputStream;

/**
 * Repository that gives read/write access to the GTFS data model being built.
 */
public interface GtfsDatasetRepository {

    /**
     * Return a GTFS agency by id.
     * @param agencyId the agency id
     * @return the GTFS agency
     */
    Agency getAgencyById(String agencyId);


    /**
     * Return a GTFS trip by id.
     * @param tripId the trip id
     * @return the GTFS trip
     */
    Trip getTripById(String tripId);

    /**
     * Return a GTFS stop by id.
     * @param stopId the stop id
     * @return the GTFS stop
     */
    Stop getStopById(String stopId);

    /**
     * Add an entity to the in-memory GTFS object model.
     * @param entity the GTFS entity to be saved.
     */
    void saveEntity(Object entity);

    /**
     * Generate a GTFS archive from the GTFS object model and return an input stream pointing to it.
     * @return the GTFS archive
     */
    InputStream writeGtfs();

    Agency getDefaultAgency();
}
