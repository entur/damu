package no.entur.damu.model;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.rutebanken.netex.model.DayType;

public class ServiceCalendarPeriod {

    private ServiceDate startDate;
    private ServiceDate endDate;


    public ServiceCalendarPeriod(DayType dayType) {
        startDate = new ServiceDate();
        endDate = new ServiceDate();
    }


    public ServiceDate getStartDate() {
        return startDate;
    }

    public ServiceDate getEndDate() {
        return endDate;
    }
}
