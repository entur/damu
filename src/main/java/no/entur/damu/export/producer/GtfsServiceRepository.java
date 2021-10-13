package no.entur.damu.export.producer;

import no.entur.damu.export.model.GtfsService;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.OperatingDay;

import java.util.Collection;
import java.util.Set;

public interface GtfsServiceRepository {
    Collection<GtfsService> getAllServices();

    GtfsService getServiceForDayTypes(Set<DayType> dayTypes);

    GtfsService getServiceForOperatingDays(Set<OperatingDay> operatingDays);
}
