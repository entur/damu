package no.entur.damu.export.producer;

import no.entur.damu.export.model.GtfsService;
import no.entur.damu.export.model.ServiceCalendarPeriod;
import org.apache.commons.lang3.StringUtils;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.PropertyOfDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GtfsServiceRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(GtfsServiceRepository.class);


    // No restrictions in GTFS spec, but restricted to suit clients
    private static final int MAX_SERVICE_ID_CHARS = 256;

    private final NetexEntitiesIndex netexTimetableEntitiesIndex;
    private final Map<String, GtfsService> gtfsServices;

    public GtfsServiceRepository(NetexEntitiesIndex netexTimetableEntitiesIndex) {
        this.netexTimetableEntitiesIndex = netexTimetableEntitiesIndex;
        this.gtfsServices = new HashMap<>();
    }

    public GtfsService getService(Set<DayType> dayTypeSet) {
        String serviceId = getServiceId(dayTypeSet);
        return gtfsServices.computeIfAbsent(serviceId, s -> createGtfsService(dayTypeSet, serviceId));
    }

    public Collection<GtfsService> getAllServices() {
        return gtfsServices.values();
    }

    private String getServiceId(Set<DayType> dayTypeSet) {

        String key = "DayType:" + dayTypeSet.stream().map(EntityStructure::getId).map(this::splitId).sorted().collect(Collectors.joining("-"));
        // Avoid to long strings. Replace truncated part by its hash to preserve a (best effort) semi uniqueness
        if (key.length() > MAX_SERVICE_ID_CHARS) {
            String tooLongPart = key.substring(MAX_SERVICE_ID_CHARS - 10);
            key = key.replace(tooLongPart, StringUtils.truncate("" + tooLongPart.hashCode(), 10));
        }
        return key;
    }

    private String splitId(String id) {
        return id.split(":")[2];
    }


    private GtfsService createGtfsService(Set<DayType> dayTypes, String serviceId) {
        int nbPeriods = countPeriods(dayTypes);
        if (nbPeriods == 0) {
            return createGtfsServiceForIndividualDates(dayTypes, serviceId);
        } else if (nbPeriods == 1) {
            return createGtfsServiceForOnePeriodAndIndividualDates(dayTypes, serviceId);
        } else {
            return createGtfsServiceForMultiplePeriodsAndIndividualDates(dayTypes, serviceId);
        }
    }

    private GtfsService createGtfsServiceForIndividualDates(Set<DayType> dayTypes, String serviceId) {
        LOGGER.debug("Creating GTFS Service for individual dates");
        GtfsService gtfsService = new GtfsService(serviceId);
        dayTypes.stream()
                .map(dayType -> netexTimetableEntitiesIndex.getDayTypeAssignmentsByDayTypeIdIndex().get(dayType.getId()))
                .flatMap(Collection::stream)
                .forEach(dayTypeAssignment -> addIndividualDate(gtfsService, dayTypeAssignment));
        return gtfsService;
    }

    private void addIndividualDate(GtfsService gtfsService, DayTypeAssignment dayTypeAssignment) {
        LocalDateTime date;
        if (dayTypeAssignment.getOperatingDayRef() != null) {
            OperatingDay operatingDay = netexTimetableEntitiesIndex.getOperatingDayIndex().get(dayTypeAssignment.getOperatingDayRef().getRef());
            date = operatingDay.getCalendarDate();
        } else if (dayTypeAssignment.getDate() != null) {
            date = dayTypeAssignment.getDate();
        } else {
            throw new IllegalStateException("Both Date and OperatingDay are undefined");
        }
        if (dayTypeAssignment.isIsAvailable() != null && !dayTypeAssignment.isIsAvailable()) {
            gtfsService.addExcludedDate(date);
        } else {
            gtfsService.addIncludedDate(date);
        }
    }

    private GtfsService createGtfsServiceForOnePeriodAndIndividualDates(Set<DayType> dayTypes, String serviceId) {
        LOGGER.debug("Creating GTFS Service for one period and individual dates");
        GtfsService gtfsService = new GtfsService(serviceId);
        DayTypeAssignment dayTypeAssignmentWithPeriod = dayTypes.stream()
                .map(dayType -> netexTimetableEntitiesIndex.getDayTypeAssignmentsByDayTypeIdIndex().get(dayType.getId()))
                .flatMap(Collection::stream)
                .filter(dta -> dta.getOperatingPeriodRef() != null)
                .findFirst()
                .orElseThrow();

        DayType dayTypeWithAPeriod = netexTimetableEntitiesIndex.getDayTypeIndex().get(dayTypeAssignmentWithPeriod.getDayTypeRef().getValue().getRef());

        if (dayTypeWithAPeriod.getProperties() != null && dayTypeWithAPeriod.getProperties().getPropertyOfDay() != null) {
            for (PropertyOfDay propertyOfDay : dayTypeWithAPeriod.getProperties().getPropertyOfDay()) {
                if (propertyOfDay.getDaysOfWeek() != null && !propertyOfDay.getDaysOfWeek().isEmpty()) {
                    gtfsService.setDaysOfWeek(propertyOfDay.getDaysOfWeek());
                }
            }
        }

        OperatingPeriod operatingPeriod = netexTimetableEntitiesIndex.getOperatingPeriodIndex().get(dayTypeAssignmentWithPeriod.getOperatingPeriodRef().getRef());
        ServiceCalendarPeriod serviceCalendarPeriod = new ServiceCalendarPeriod(operatingPeriod.getFromDate(), operatingPeriod.getToDate());
        gtfsService.setServiceCalendarPeriod(serviceCalendarPeriod);

        dayTypes.stream()
                .map(dayType -> netexTimetableEntitiesIndex.getDayTypeAssignmentsByDayTypeIdIndex().get(dayType.getId()))
                .flatMap(Collection::stream)
                .filter(dta -> dta.getOperatingPeriodRef() == null)
                .forEach(dayTypeAssignment -> addIndividualDate(gtfsService, dayTypeAssignment));

        return gtfsService;
    }

    private GtfsService createGtfsServiceForMultiplePeriodsAndIndividualDates(Set<DayType> dayTypes, String serviceId) {
        LOGGER.debug("Creating GTFS Service for multiple periods and individual dates");
        return new GtfsService(serviceId);
    }


    private int countPeriods(Set<DayType> dayTypes) {
        return dayTypes.stream().map(dayType -> netexTimetableEntitiesIndex.getDayTypeAssignmentsByDayTypeIdIndex()
                .get(dayType.getId())
                .stream()
                .filter(dayTypeAssignment -> dayTypeAssignment.getOperatingPeriodRef() != null)
                .count()).mapToInt(Long::intValue).sum();
    }


}
