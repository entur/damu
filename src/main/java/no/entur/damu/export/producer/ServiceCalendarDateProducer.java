package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.ServiceCalendarDate;

import java.time.LocalDateTime;

public interface ServiceCalendarDateProducer {
    ServiceCalendarDate produce(String serviceId, LocalDateTime date, boolean isAvailable);
}
