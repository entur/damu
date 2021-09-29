package no.entur.damu.export.producer;

import no.entur.damu.export.util.DestinationDisplayUtil;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

import static no.entur.damu.export.util.GtfsUtil.toGtfsId;

public class TripProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TripProducer.class);


    private final Agency agency;
    private final GtfsServiceRepository gtfsServiceRepository;
    private final NetexEntitiesIndex netexTimetableEntitiesIndex;

    public TripProducer(Agency agency, GtfsServiceRepository gtfsServiceRepository, NetexEntitiesIndex netexTimetableEntitiesIndex) {
        this.agency = agency;
        this.gtfsServiceRepository = gtfsServiceRepository;
        this.netexTimetableEntitiesIndex = netexTimetableEntitiesIndex;
    }


    public Trip produce(ServiceJourney serviceJourney, JourneyPattern journeyPattern, Route gtfsRoute, AgencyAndId shapeId, DestinationDisplay startDestinationDisplay) {
        String tripId = toGtfsId(serviceJourney.getId(), null, true);

        AgencyAndId tripAgencyAndId = new AgencyAndId();
        tripAgencyAndId.setId(tripId);
        tripAgencyAndId.setAgencyId(agency.getId());
        Trip trip = new Trip();
        trip.setId(tripAgencyAndId);

        AgencyAndId serviceAgencyAndId = new AgencyAndId();

        if (serviceJourney.getDayTypes() != null) {
            Set<DayType> dayTypes = serviceJourney.getDayTypes()
                    .getDayTypeRef()
                    .stream()
                    .map(jaxbElement -> jaxbElement.getValue().getRef())
                    .map(netexTimetableEntitiesIndex.getDayTypeIndex()::get)
                    .collect(Collectors.toSet());
            serviceAgencyAndId.setId(gtfsServiceRepository.getServiceForDayTypes(dayTypes).getId());
        } else {
            LOGGER.debug("Producing trip based on DatedServiceJourneys for ServiceJourney {}", serviceJourney.getId());
            // DatedServiceJourneys for cancelled and replaced trips are filtered out
            Set<OperatingDay> operatingDays = netexTimetableEntitiesIndex.getDatedServiceJourneyByServiceJourneyRefIndex()
                    .get(serviceJourney.getId())
                    .stream()
                    .filter(datedServiceJourney -> {
                        ServiceAlterationEnumeration serviceAlteration = datedServiceJourney.getServiceAlteration();
                        return ServiceAlterationEnumeration.CANCELLATION != serviceAlteration && ServiceAlterationEnumeration.REPLACED != serviceAlteration;
                    })
                    .map(datedServiceJourney -> netexTimetableEntitiesIndex.getOperatingDayIndex().get(datedServiceJourney.getOperatingDayRef().getRef()))
                    .collect(Collectors.toSet());
            serviceAgencyAndId.setId(gtfsServiceRepository.getServiceForOperatingDays(operatingDays).getId());
        }

        if (startDestinationDisplay != null) {
            trip.setTripHeadsign(DestinationDisplayUtil.getFrontTextWithComputedVias(startDestinationDisplay, netexTimetableEntitiesIndex));
        } else if (serviceJourney.getName() != null) {
            trip.setTripHeadsign(serviceJourney.getName().getValue());
        } else if (journeyPattern.getName() != null) {
            trip.setTripHeadsign(journeyPattern.getName().getValue());
        } else {
            LOGGER.warn("Missing trip head sign for ServiceJourney {}", serviceJourney.getId());
        }
        serviceAgencyAndId.setAgencyId(agency.getId());
        trip.setServiceId(serviceAgencyAndId);
        trip.setRoute(gtfsRoute);
        trip.setShapeId(shapeId);
        return trip;

    }

}
