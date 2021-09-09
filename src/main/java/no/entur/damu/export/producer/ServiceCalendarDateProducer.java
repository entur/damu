package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class ServiceCalendarDateProducer {

    private final Agency agency;

    public ServiceCalendarDateProducer(Agency agency) {
        this.agency = agency;
    }

    public ServiceCalendarDate produce(String serviceId, LocalDateTime date, boolean isAvailable) {
        ServiceCalendarDate serviceCalendarDate = new ServiceCalendarDate();
        AgencyAndId serviceCalendarDateAgencyAndId = new AgencyAndId();
        serviceCalendarDateAgencyAndId.setId(serviceId);
        serviceCalendarDateAgencyAndId.setAgencyId(agency.getId());
        serviceCalendarDate.setServiceId(serviceCalendarDateAgencyAndId);
        serviceCalendarDate.setDate(new ServiceDate(toGtfsDate(date)));
        serviceCalendarDate.setExceptionType(isAvailable ? 1 : 2);

        return serviceCalendarDate;
    }

    private static Date toGtfsDate(LocalDateTime netexDate) {
        return Date.from(netexDate.atZone(ZoneId.systemDefault()).toInstant());
    }


}