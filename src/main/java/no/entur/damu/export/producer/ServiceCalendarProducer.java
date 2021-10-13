package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

import java.util.Collection;

public interface ServiceCalendarProducer {
    ServiceCalendar produce(String serviceId, ServiceDate startDate, ServiceDate endDate, Collection<DayOfWeekEnumeration> daysOfWeeks);
}
