package no.entur.damu.export.model;

import org.onebusaway.gtfs.model.calendar.ServiceDate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class ServiceCalendarPeriod {

    private final ServiceDate startDate;
    private final ServiceDate endDate;


    public ServiceCalendarPeriod(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        startDate = new ServiceDate(toGtfsDate(startDateTime));
        endDate = new ServiceDate(toGtfsDate(endDateTime));
    }


    public ServiceDate getStartDate() {
        return startDate;
    }

    public ServiceDate getEndDate() {
        return endDate;
    }


    private static Date toGtfsDate(LocalDateTime netexDate) {
        return Date.from(netexDate.atZone(ZoneId.systemDefault()).toInstant());
    }
}
