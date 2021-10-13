package no.entur.damu.export.repository;

import no.entur.damu.export.exception.NetexParsingException;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.LocaleStructure;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.VersionFrameDefaultsStructure;
import org.rutebanken.netex.model.VersionFrame_VersionStructure;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultNetexDatasetRepository implements NetexDatasetRepository {

    private final NetexEntitiesIndex netexEntitiesIndex;

    public DefaultNetexDatasetRepository() {
        this.netexEntitiesIndex = new NetexEntitiesIndexImpl();
    }

    @Override
    public NetexEntitiesIndex getIndex() {
        return netexEntitiesIndex;
    }

    @Override
    public OperatingDay getOperatingDayByDayTypeAssignment(DayTypeAssignment dayTypeAssignment) {
        if (dayTypeAssignment.getOperatingDayRef() == null) {
            return null;
        }
        return netexEntitiesIndex.getOperatingDayIndex().get(dayTypeAssignment.getOperatingDayRef().getRef());
    }

    @Override
    public OperatingPeriod getOperatingPeriodByDayTypeAssignment(DayTypeAssignment dayTypeAssignment) {
        if (dayTypeAssignment.getOperatingPeriodRef() == null) {
            return null;
        }
        return netexEntitiesIndex.getOperatingPeriodIndex().get(dayTypeAssignment.getOperatingPeriodRef().getRef());
    }

    @Override
    public DayType getDayTypeByDayTypeAssignment(DayTypeAssignment dayTypeAssignment) {
        return netexEntitiesIndex.getDayTypeIndex().get(dayTypeAssignment.getDayTypeRef().getValue().getRef());
    }

    @Override
    public String getTimeZone() {
        Set<String> timeZones = netexEntitiesIndex.getCompositeFrames()
                .stream()
                .map(VersionFrame_VersionStructure::getFrameDefaults)
                .filter(Objects::nonNull)
                .map(VersionFrameDefaultsStructure::getDefaultLocale)
                .filter(Objects::nonNull)
                .map(LocaleStructure::getTimeZone)
                .collect(Collectors.toSet());

        if (timeZones.size() > 1) {
            throw new NetexParsingException("The dataset contains more than one default timezone");
        }

        return timeZones.stream().findFirst().orElseThrow(() -> new NetexParsingException("The dataset does not contain a default timezone"));
    }


    @Override
    public String getAuthorityIdForLine(Line line) {
        Network network = findNetwork(line.getRepresentedByGroupRef().getRef());
        return network.getTransportOrganisationRef().getValue().getRef();
    }

    /**
     * Return the network referenced by the <RepresentedByGroupRef>.
     * RepresentedByGroupRef can reference a network either directly or indirectly (through a group of lines)
     *
     * @param networkOrGroupOfLinesRef reference to a Network or a group of lines.
     * @return the network itself or the network to which the group of lines belongs to.
     */
    private Network findNetwork(String networkOrGroupOfLinesRef) {
        Network network = netexEntitiesIndex.getNetworkIndex().get(networkOrGroupOfLinesRef);
        if (network != null) {
            return network;
        } else {
            return netexEntitiesIndex.getNetworkIndex()
                    .getAll()
                    .stream()
                    .filter(n -> n.getGroupsOfLines() != null)
                    .filter(n -> n.getGroupsOfLines()
                            .getGroupOfLines()
                            .stream()
                            .anyMatch(groupOfLine -> groupOfLine.getId().equals(networkOrGroupOfLinesRef)))
                    .findFirst()
                    .orElseThrow();
        }
    }

    @Override
    public Collection<Line> getLines() {
        return netexEntitiesIndex.getLineIndex().getAll();
    }

    @Override
    public Collection<ServiceJourney> getServiceJourneys() {
        return netexEntitiesIndex.getServiceJourneyIndex().getAll();
    }

    @Override
    public Collection<ServiceJourneyInterchange> getServiceJourneyInterchanges() {
        return netexEntitiesIndex.getServiceJourneyInterchangeIndex().getAll();
    }

    @Override
    public Collection<DayTypeAssignment> getDayTypeAssignmentsByDayType(DayType dayType) {
        return netexEntitiesIndex.getDayTypeAssignmentsByDayTypeIdIndex().get(dayType.getId());
    }


    @Override
    public Authority getAuthorityById(String authorityId) {
        return netexEntitiesIndex.getAuthorityIndex().get(authorityId);
    }

    @Override
    public ServiceJourney getServiceJourneyById(String serviceJourneyId) {
        return netexEntitiesIndex.getServiceJourneyIndex().get(serviceJourneyId);
    }

    @Override
    public String getFlexibleStopPlaceIdByScheduledStopPointId(String scheduledStopPointId) {
        return netexEntitiesIndex.getFlexibleStopPlaceIdByStopPointRefIndex().get(scheduledStopPointId);
    }

    @Override
    public String getQuayIdByScheduledStopPointId(String scheduledStopPointId) {
        return netexEntitiesIndex.getQuayIdByStopPointRefIndex().get(scheduledStopPointId);
    }


    @Override
    public JourneyPattern getJourneyPatternById(String journeyPatternId) {
        return netexEntitiesIndex.getJourneyPatternIndex().get(journeyPatternId);
    }

    @Override
    public Collection<ServiceJourney> getServiceJourneysByJourneyPattern(JourneyPattern journeyPattern) {
        return getServiceJourneys()
                .stream()
                .filter(serviceJourney -> serviceJourney.getJourneyPatternRef().getValue().getRef().equals(journeyPattern.getId()))
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<org.rutebanken.netex.model.Route> getRoutesByLine(Line line) {
        return netexEntitiesIndex.getRouteIndex().getAll()
                .stream()
                .filter(route -> route.getLineRef().getValue().getRef().equals(line.getId()))
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<JourneyPattern> getJourneyPatternByRoute(org.rutebanken.netex.model.Route route) {
        return netexEntitiesIndex.getJourneyPatternIndex().getAll()
                .stream()
                .filter(journeyPattern -> journeyPattern.getRouteRef().getRef().equals(route.getId()))
                .collect(Collectors.toSet());
    }

    @Override
    public ServiceLink getServiceLinkById(String serviceLinkId) {
        return netexEntitiesIndex.getServiceLinkIndex().get(serviceLinkId);
    }


    @Override
    public DestinationDisplay getDestinationDisplayById(String destinationDisplayId) {
        return netexEntitiesIndex.getDestinationDisplayIndex().get(destinationDisplayId);
    }

    @Override
    public Collection<DatedServiceJourney> getDatedServiceJourneysByServiceJourneyId(String serviceJourneyId) {
        return netexEntitiesIndex.getDatedServiceJourneyByServiceJourneyRefIndex().get(serviceJourneyId);
    }

    @Override
    public DayType getDayTypeById(String dayTypeId) {
        return netexEntitiesIndex.getDayTypeIndex().get(dayTypeId);
    }

    @Override
    public OperatingDay getOperatingDayById(String operatingDayId) {
        return netexEntitiesIndex.getOperatingDayIndex().get(operatingDayId);
    }

}
