package no.entur.damu.export.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A GTFS service representing an operating period and/or a set of explicitly included/excluded dates
 */
public class GtfsService {

    private final String id;

    private ServiceCalendarPeriod serviceCalendarPeriod;
    private final Set<LocalDateTime> includedDates = new HashSet<>();
    private final Set<LocalDateTime> excludedDates = new HashSet<>();

    public GtfsService(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void addExcludedDate(LocalDateTime date) {
        excludedDates.add(date);
    }

    public void addIncludedDate(LocalDateTime date) {
        includedDates.add(date);
    }

    public Set<LocalDateTime> getIncludedDates() {
        return new HashSet<>(includedDates);
    }

    public Set<LocalDateTime> getExcludedDates() {
        return new HashSet<>(excludedDates);
    }

    public ServiceCalendarPeriod getServiceCalendarPeriod() {
        return serviceCalendarPeriod;
    }


    public void setServiceCalendarPeriod(ServiceCalendarPeriod serviceCalendarPeriod) {
        this.serviceCalendarPeriod = serviceCalendarPeriod;
    }
}
