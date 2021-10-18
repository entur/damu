package no.entur.damu.export.repository;

import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.ServiceLink;

import java.util.Collection;

/**
 * Repository giving read access to the input NeTEx dataset.
 */
public interface NetexDatasetRepository {

    NetexEntitiesIndex getIndex();

    Collection<ServiceJourneyInterchange> getServiceJourneyInterchanges();

    Collection<DayTypeAssignment> getDayTypeAssignmentsByDayType(DayType dayType);

    OperatingDay getOperatingDayByDayTypeAssignment(DayTypeAssignment dayTypeAssignment);

    OperatingPeriod getOperatingPeriodByDayTypeAssignment(DayTypeAssignment dayTypeAssignment);

    DayType getDayTypeByDayTypeAssignment(DayTypeAssignment dayTypeAssignment);

    /**
     * Return the dataset default timezone
     * This is the timezone set at the CompositeFrame level.
     *
     * @return the dataset default timezone
     * @throws no.entur.damu.export.exception.GtfsExportException if there is no default timezone or if there is more than one default timezone.
     */
    String getTimeZone();

    /**
     * Return the authority id for a given line.
     * This is the authority of the network or group of lines referenced by the line.
     *
     * @param line a NeTEx line
     * @return the line authority
     */
    String getAuthorityIdForLine(Line line);

    Collection<Line> getLines();

    Authority getAuthorityById(String authorityId);

    ServiceJourney getServiceJourneyById(String serviceJourneyId);

    String getFlexibleStopPlaceIdByScheduledStopPointId(String scheduledStopPointId);

    String getQuayIdByScheduledStopPointId(String scheduledStopPointId);

    Collection<ServiceJourney> getServiceJourneys();

    JourneyPattern getJourneyPatternById(String journeyPatternId);

    Collection<ServiceJourney> getServiceJourneysByJourneyPattern(JourneyPattern journeyPattern);

    Collection<Route> getRoutesByLine(Line line);

    Collection<JourneyPattern> getJourneyPatternsByRoute(Route route);

    ServiceLink getServiceLinkById(String serviceLinkId);

    DestinationDisplay getDestinationDisplayById(String destinationDisplayId);

    DayType getDayTypeById(String dayTypeId);

    Collection<DatedServiceJourney> getDatedServiceJourneysByServiceJourneyId(String serviceJourneyId);

    OperatingDay getOperatingDayById(String operatingDayId);

}
