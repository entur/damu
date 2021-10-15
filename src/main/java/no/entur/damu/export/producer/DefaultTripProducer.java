package no.entur.damu.export.producer;

import no.entur.damu.export.repository.GtfsDatasetRepository;
import no.entur.damu.export.repository.NetexDatasetRepository;
import no.entur.damu.export.util.DestinationDisplayUtil;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.DirectionTypeEnumeration;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Produce a GTFS Trip or null if the service journey does not correspond to a valid TFS Trip
 * In particular ServiceJourney having a ServiceAlteration=cancelled or ServiceAlteration=replaced are not valid GTFS Trip.
 */
public class DefaultTripProducer implements TripProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTripProducer.class);
    private static final String GTFS_DIRECTION_OUTBOUND = "0";
    private static final String GTFS_DIRECTION_INBOUND = "1";


    private final Agency agency;
    private final GtfsServiceRepository gtfsServiceRepository;
    private final NetexDatasetRepository netexDatasetRepository;

    public DefaultTripProducer(NetexDatasetRepository netexDatasetRepository, GtfsDatasetRepository gtfsDatasetRepository, GtfsServiceRepository gtfsServiceRepository) {
        this.agency = gtfsDatasetRepository.getDefaultAgency();
        this.gtfsServiceRepository = gtfsServiceRepository;
        this.netexDatasetRepository = netexDatasetRepository;
    }


    /**
     * Return a GTFS Trip corresponding to the ServiceJourney or null if the ServiceJourney cannot be converted into a valid GTFS trip.
     *
     * @param serviceJourney
     * @param journeyPattern
     * @param netexRoute
     * @param gtfsRoute
     * @param shapeId
     * @param startDestinationDisplay
     * @return a GTFS Trip corresponding to the ServiceJourney or null if the ServiceJourney cannot be converted into a valid GTFS trip.
     */
    @Override
    public Trip produce(ServiceJourney serviceJourney, JourneyPattern journeyPattern, Route netexRoute, org.onebusaway.gtfs.model.Route gtfsRoute, AgencyAndId shapeId, DestinationDisplay startDestinationDisplay) {

        // Cancelled or replaced service journeys are not valid GTFS trips.
        if (ServiceAlterationEnumeration.CANCELLATION == serviceJourney.getServiceAlteration() || ServiceAlterationEnumeration.REPLACED == serviceJourney.getServiceAlteration()) {
            return null;
        }


        String tripId = serviceJourney.getId();

        AgencyAndId tripAgencyAndId = new AgencyAndId();
        tripAgencyAndId.setId(tripId);
        tripAgencyAndId.setAgencyId(agency.getId());
        Trip trip = new Trip();
        trip.setId(tripAgencyAndId);

        AgencyAndId serviceAgencyAndId = new AgencyAndId();

        // route
        trip.setRoute(gtfsRoute);

        // direction
        DirectionTypeEnumeration directionType = netexRoute.getDirectionType();
        if (DirectionTypeEnumeration.INBOUND == directionType) {
            trip.setDirectionId(GTFS_DIRECTION_INBOUND);
        } else {
            trip.setDirectionId(GTFS_DIRECTION_OUTBOUND);
        }

        // service
        if (serviceJourney.getDayTypes() != null) {
            Set<DayType> dayTypes = serviceJourney.getDayTypes()
                    .getDayTypeRef()
                    .stream()
                    .map(jaxbElement -> jaxbElement.getValue().getRef())
                    .map(netexDatasetRepository::getDayTypeById)
                    .collect(Collectors.toSet());
            serviceAgencyAndId.setId(gtfsServiceRepository.getServiceForDayTypes(dayTypes).getId());
        } else {
            LOGGER.trace("Producing trip based on DatedServiceJourneys for ServiceJourney {}", serviceJourney.getId());
            // DatedServiceJourneys for cancelled and replaced trips are filtered out
            Set<OperatingDay> operatingDays = netexDatasetRepository.getDatedServiceJourneysByServiceJourneyId(serviceJourney.getId())
                    .stream()
                    .filter(datedServiceJourney -> {
                        ServiceAlterationEnumeration serviceAlteration = datedServiceJourney.getServiceAlteration();
                        return ServiceAlterationEnumeration.CANCELLATION != serviceAlteration && ServiceAlterationEnumeration.REPLACED != serviceAlteration;
                    })
                    .map(datedServiceJourney -> netexDatasetRepository.getOperatingDayById(datedServiceJourney.getOperatingDayRef().getRef()))
                    .collect(Collectors.toSet());
            serviceAgencyAndId.setId(gtfsServiceRepository.getServiceForOperatingDays(operatingDays).getId());
        }
        serviceAgencyAndId.setAgencyId(agency.getId());
        trip.setServiceId(serviceAgencyAndId);

        // destination display = head sign
        if (startDestinationDisplay != null) {
            trip.setTripHeadsign(DestinationDisplayUtil.getFrontTextWithComputedVias(startDestinationDisplay, netexDatasetRepository));
        } else if (serviceJourney.getName() != null) {
            trip.setTripHeadsign(serviceJourney.getName().getValue());
        } else if (journeyPattern.getName() != null) {
            trip.setTripHeadsign(journeyPattern.getName().getValue());
        } else {
            LOGGER.warn("Missing trip head sign for ServiceJourney {}", serviceJourney.getId());
        }

        // shape
        trip.setShapeId(shapeId);

        return trip;

    }

}
