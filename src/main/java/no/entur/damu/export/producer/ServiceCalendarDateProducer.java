package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.ServiceCalendarDate;

import java.time.LocalDateTime;


/**
 * Produce a GTFS Service Calendar Date.
 */
public interface ServiceCalendarDateProducer {

    /**
     * Produce a GTFS Service Calendar Date for a given GTFS service.
     * @param serviceId the id of the GTFS service
     * @param date the service date.
     * @param isAvailable true if the service runs on this date.
     * @return a GTFS Service Calendar Date for the given GTFS service.
     */
    ServiceCalendarDate produce(String serviceId, LocalDateTime date, boolean isAvailable);
}
