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

/**
 * Create and store the GTFS services for the current dataset.
 * GTFS services are created while iterating through ServiceJourneys and DatedServiceJourneys.
 * The GTFS services are de-duplicated by creating a unique ID per set of DayTypes (trips based on ServiceJourneys) or set of OperatingDays (trips based on DatedServiceJourneys)
 */
public class GtfsServiceRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(GtfsServiceRepository.class);


    // No restrictions in GTFS spec, but restricted to suit clients
    private static final int MAX_SERVICE_ID_CHARS = 256;

    private final String codespace;
    private final NetexEntitiesIndex netexTimetableEntitiesIndex;
    private final Map<String, GtfsService> gtfsServices;

    public GtfsServiceRepository(String codespace, NetexEntitiesIndex netexTimetableEntitiesIndex) {
        this.netexTimetableEntitiesIndex = netexTimetableEntitiesIndex;
        this.gtfsServices = new HashMap<>();
        this.codespace = codespace;
    }

    public Collection<GtfsService> getAllServices() {
        return gtfsServices.values();
    }

    /**
     * Create a service for a set of DayTypes.
     * This is used for creating trips based on ServiceJourneys (not DatedServiceJourneys.
     *
     * @param dayTypes dayTypes for the service.
     * @return a service running on the days specified by the provided DayTypes.
     */
    public GtfsService getServiceForDayTypes(Set<DayType> dayTypes) {
        String serviceId = getServiceIdForDayTypes(dayTypes);
        return gtfsServices.computeIfAbsent(serviceId, s -> createGtfsServiceForDayTypes(dayTypes, serviceId));
    }

    /**
     * Create a service for a set of operating days.
     * This is used for creating trips based on DatedServiceJourneys.
     *
     * @param operatingDays operating days for the service.
     * @return a service running on the given operating days.
     */
    public GtfsService getServiceForOperatingDays(Set<OperatingDay> operatingDays) {
        String serviceId = getServiceIdForOperatingDays(operatingDays);
        return gtfsServices.computeIfAbsent(serviceId, s -> createGtfsServiceForOperatingDays(operatingDays, serviceId));
    }

    private String getServiceIdForDayTypes(Set<DayType> dayTypes) {
        String key = codespace + ":DayType:" + dayTypes.stream().map(EntityStructure::getId).map(this::splitId).sorted().collect(Collectors.joining("-"));
        // Avoid too long strings. Replace truncated part by its hash to preserve a (best effort) semi uniqueness
        if (key.length() > MAX_SERVICE_ID_CHARS) {
            String tooLongPart = key.substring(MAX_SERVICE_ID_CHARS - 10);
            key = key.replace(tooLongPart, StringUtils.truncate("" + tooLongPart.hashCode(), 10));
        }
        return key;
    }

    private String getServiceIdForOperatingDays(Set<OperatingDay> operatingDays) {
        String key = codespace + ":OperatingDay:" + operatingDays.stream().map(EntityStructure::getId).map(this::splitId).sorted().collect(Collectors.joining("-"));
        // Avoid too long strings. Replace truncated part by its hash to preserve a (best effort) semi uniqueness
        if (key.length() > MAX_SERVICE_ID_CHARS) {
            String tooLongPart = key.substring(MAX_SERVICE_ID_CHARS - 10);
            key = key.replace(tooLongPart, StringUtils.truncate("" + tooLongPart.hashCode(), 10));
        }
        return key;
    }


    private String splitId(String id) {
        return id.split(":")[2];
    }


    private GtfsService createGtfsServiceForDayTypes(Set<DayType> dayTypes, String serviceId) {
        int nbPeriods = countPeriods(dayTypes);
        if (nbPeriods == 0) {
            return createGtfsServiceForIndividualDates(dayTypes, serviceId);
        } else if (nbPeriods == 1) {
            return createGtfsServiceForOnePeriodAndIndividualDates(dayTypes, serviceId);
        } else {
            return createGtfsServiceForMultiplePeriodsAndIndividualDates(dayTypes, serviceId);
        }
    }

    private GtfsService createGtfsServiceForOperatingDays(Set<OperatingDay> operatingDays, String serviceId) {
        LOGGER.debug("Creating GTFS Service for operating days for serviceId {}", serviceId);
        GtfsService gtfsService = new GtfsService(serviceId);
        operatingDays.forEach(operatingDay -> gtfsService.addIncludedDate(operatingDay.getCalendarDate()));
        return gtfsService;
    }

    private GtfsService createGtfsServiceForIndividualDates(Set<DayType> dayTypes, String serviceId) {
        LOGGER.debug("Creating GTFS Service for individual dates for serviceId {}", serviceId);
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
        LOGGER.debug("Creating GTFS Service for one period and individual dates for serviceId {}", serviceId);
        GtfsService gtfsService = new GtfsService(serviceId);
        DayTypeAssignment dayTypeAssignmentWithPeriod = dayTypes.stream()
                .map(dayType -> netexTimetableEntitiesIndex.getDayTypeAssignmentsByDayTypeIdIndex().get(dayType.getId()))
                .flatMap(Collection::stream)
                .filter(dta -> dta.getOperatingPeriodRef() != null)
                .findFirst()
                .orElseThrow();

        DayType dayTypeWithAPeriod = netexTimetableEntitiesIndex.getDayTypeIndex().get(dayTypeAssignmentWithPeriod.getDayTypeRef().getValue().getRef());


        OperatingPeriod operatingPeriod = netexTimetableEntitiesIndex.getOperatingPeriodIndex().get(dayTypeAssignmentWithPeriod.getOperatingPeriodRef().getRef());
        ServiceCalendarPeriod serviceCalendarPeriod = new ServiceCalendarPeriod(operatingPeriod.getFromDate(), operatingPeriod.getToDate());
        if (dayTypeWithAPeriod.getProperties() != null && dayTypeWithAPeriod.getProperties().getPropertyOfDay() != null) {
            for (PropertyOfDay propertyOfDay : dayTypeWithAPeriod.getProperties().getPropertyOfDay()) {
                if (propertyOfDay.getDaysOfWeek() != null && !propertyOfDay.getDaysOfWeek().isEmpty()) {
                    serviceCalendarPeriod.setDaysOfWeek(propertyOfDay.getDaysOfWeek());
                }
            }
        }

        gtfsService.setServiceCalendarPeriod(serviceCalendarPeriod);

        dayTypes.stream()
                .map(dayType -> netexTimetableEntitiesIndex.getDayTypeAssignmentsByDayTypeIdIndex().get(dayType.getId()))
                .flatMap(Collection::stream)
                .filter(dta -> dta.getOperatingPeriodRef() == null)
                .forEach(dayTypeAssignment -> addIndividualDate(gtfsService, dayTypeAssignment));

        return gtfsService;
    }

    private GtfsService createGtfsServiceForMultiplePeriodsAndIndividualDates(Set<DayType> dayTypes, String serviceId) {
        LOGGER.debug("Creating GTFS Service for multiple periods and individual dates  for serviceId {}", serviceId);
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
