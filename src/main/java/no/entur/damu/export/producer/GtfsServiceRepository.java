package no.entur.damu.export.producer;

import no.entur.damu.export.model.GtfsService;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.OperatingDay;

import java.util.Collection;
import java.util.Set;


/**
 * Repository giving access to the list of GTFS service in the GTFS object model being built.
 */
public interface GtfsServiceRepository {

    /**
     * Return the list of services.
     * @return the list of services.
     */
    Collection<GtfsService> getAllServices();

    /**
     * Create or retrieve the GTFS service corresponding to a set of DayTypes.
     * Multiple calls to this method with the same set of day types return the same object.
     * @param dayTypes the set of NeTEx DayTypes.
     * @return the GTFS service for this set of DayTypes.
     */
    GtfsService getServiceForDayTypes(Set<DayType> dayTypes);

    /**
     * Create or retrieve the GTFS service corresponding to a set of OperatingDays.
     * Multiple calls to this method with the same set of operating days return the same object.
     * @param operatingDays the set of NeTEx OperatingDays.
     * @return the GTFS service for this set of OperatingDays.
     */
    GtfsService getServiceForOperatingDays(Set<OperatingDay> operatingDays);
}
