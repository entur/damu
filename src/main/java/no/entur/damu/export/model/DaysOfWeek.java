package no.entur.damu.export.model;

import org.onebusaway.gtfs.model.ServiceCalendar;
import org.rutebanken.netex.model.DayOfWeekEnumeration;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.PropertyOfDay;

import java.util.List;

public class DaysOfWeek {

    private static final List<DayOfWeekEnumeration> ALL_DAY_OF_WEEK_ENUMERATIONS = List.of(DayOfWeekEnumeration.EVERYDAY);

    private List<DayOfWeekEnumeration> daysOfWeeks;

    public DaysOfWeek(DayType dayType) {
        if (dayType.getProperties() != null && dayType.getProperties().getPropertyOfDay() != null) {
            for (PropertyOfDay propertyOfDay : dayType.getProperties().getPropertyOfDay()) {
                if (propertyOfDay.getDaysOfWeek() != null && !propertyOfDay.getDaysOfWeek().isEmpty()) {
                    this.daysOfWeeks = propertyOfDay.getDaysOfWeek();
                }
            }
        }

        // By default all days are active
        if (this.daysOfWeeks == null) {
            this.daysOfWeeks = ALL_DAY_OF_WEEK_ENUMERATIONS;
        }
    }

    public void setDays(ServiceCalendar serviceCalendar) {
        if (daysOfWeeks.contains(DayOfWeekEnumeration.EVERYDAY)) {
            serviceCalendar.setMonday(1);
            serviceCalendar.setTuesday(1);
            serviceCalendar.setWednesday(1);
            serviceCalendar.setThursday(1);
            serviceCalendar.setFriday(1);
            serviceCalendar.setSaturday(1);
            serviceCalendar.setSunday(1);
            return;
        }

        serviceCalendar.setMonday(daysOfWeeks.contains(DayOfWeekEnumeration.MONDAY) ? 1 : 0);
        serviceCalendar.setTuesday(daysOfWeeks.contains(DayOfWeekEnumeration.TUESDAY) ? 1 : 0);
        serviceCalendar.setWednesday(daysOfWeeks.contains(DayOfWeekEnumeration.WEDNESDAY) ? 1 : 0);
        serviceCalendar.setThursday(daysOfWeeks.contains(DayOfWeekEnumeration.THURSDAY) ? 1 : 0);
        serviceCalendar.setFriday(daysOfWeeks.contains(DayOfWeekEnumeration.FRIDAY) ? 1 : 0);
        serviceCalendar.setSaturday(daysOfWeeks.contains(DayOfWeekEnumeration.SATURDAY) ? 1 : 0);
        serviceCalendar.setSunday(daysOfWeeks.contains(DayOfWeekEnumeration.SUNDAY) ? 1 : 0);

        if (daysOfWeeks.contains(DayOfWeekEnumeration.WEEKDAYS)) {
            serviceCalendar.setMonday(1);
            serviceCalendar.setTuesday(1);
            serviceCalendar.setWednesday(1);
            serviceCalendar.setThursday(1);
            serviceCalendar.setFriday(1);
        }

        if (daysOfWeeks.contains(DayOfWeekEnumeration.WEEKEND)) {
            serviceCalendar.setSaturday(1);
            serviceCalendar.setSunday(1);
        }
    }
}
