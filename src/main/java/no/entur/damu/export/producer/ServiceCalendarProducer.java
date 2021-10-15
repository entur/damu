package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

import java.util.Collection;


/**
 * Produce a GTFS Service Calendar.
 */
public interface ServiceCalendarProducer {

    /**
     * Produce a GTFS Service Calendar for a given GTFS service.
     * @param serviceId the id of the GTFS service.
     * @param startDate the start date of the calendar period.
     * @param endDate the end date of the calendar period.
     * @param daysOfWeeks the days of week on which the service runs. If empty the service runs every day of the week.
     * @return the GTFS service calendar for this GTFS service.
     */
    ServiceCalendar produce(String serviceId, ServiceDate startDate, ServiceDate endDate, Collection<DayOfWeekEnumeration> daysOfWeeks);
}
