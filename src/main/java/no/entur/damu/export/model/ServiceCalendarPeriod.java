package no.entur.damu.export.model;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * A GTFS service calendar period defined by an interval [startDate, endDate] and a set of days of week for which the service is running.
 * If no days of week are specified then the service runs every day of the week.
 */
public class ServiceCalendarPeriod {

    private final ServiceDate startDate;
    private final ServiceDate endDate;

    private List<DayOfWeekEnumeration> daysOfWeek;

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

    public List<DayOfWeekEnumeration> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<DayOfWeekEnumeration> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }


    private static Date toGtfsDate(LocalDateTime netexDate) {
        return Date.from(netexDate.atZone(ZoneId.systemDefault()).toInstant());
    }
}
