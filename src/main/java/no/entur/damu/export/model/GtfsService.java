package no.entur.damu.export.model;

import org.rutebanken.netex.model.DayOfWeekEnumeration;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GtfsService {

    private final String id;


    private ServiceCalendarPeriod  serviceCalendarPeriod;
    private List<DayOfWeekEnumeration> daysOfWeek;
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

    public Collection<LocalDateTime> getIncludedDates() {
        return new HashSet<>(includedDates);
    }

    public Collection<LocalDateTime> getExcludedDates() {
        return new HashSet<>(excludedDates);
    }

    public ServiceCalendarPeriod getServiceCalendarPeriod() {
        return serviceCalendarPeriod;
    }

    public List<DayOfWeekEnumeration> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<DayOfWeekEnumeration> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public void setServiceCalendarPeriod(ServiceCalendarPeriod serviceCalendarPeriod) {
        this.serviceCalendarPeriod = serviceCalendarPeriod;
    }
}
