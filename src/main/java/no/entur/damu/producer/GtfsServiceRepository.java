package no.entur.damu.producer;

import no.entur.damu.model.GtfsService;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.EntityStructure;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GtfsServiceRepository {

    // No restrictions in GTFS spec, but restricted to suit clients
    private static final int MAX_SERVICE_ID_CHARS = 256;

    private final Map<String, GtfsService> gtfsServices = new HashMap<>();


    public GtfsService getService(Set<DayType> dayTypeSet) {
        String serviceId = getServiceId(dayTypeSet);
        return gtfsServices.computeIfAbsent(serviceId, s -> createGtfsService(dayTypeSet, serviceId));
    }

    private GtfsService createGtfsService(Set<DayType> dayTypeSet, String serviceId) {

        return new GtfsService(serviceId);
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
}
