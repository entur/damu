package no.entur.damu.producer;

import org.entur.netex.index.api.NetexEntitiesIndex;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.ServiceJourney;

import java.util.Set;
import java.util.stream.Collectors;

import static no.entur.damu.util.GtfsUtil.toGtfsId;

public class TripProducer {

    private final Agency agency;
    private final Route route;
    private final GtfsServiceRepository gtfsServiceRepository;
    private final NetexEntitiesIndex netexTimetableEntitiesIndex;

    public TripProducer(Agency agency, Route route, GtfsServiceRepository gtfsServiceRepository, NetexEntitiesIndex netexTimetableEntitiesIndex) {
        this.agency = agency;
        this.route = route;
        this.gtfsServiceRepository = gtfsServiceRepository;
        this.netexTimetableEntitiesIndex = netexTimetableEntitiesIndex;
    }


    public Trip produce(ServiceJourney serviceJourney) {
        String tripId = toGtfsId(serviceJourney.getId(), null, true);

        AgencyAndId tripAgencyAndId = new AgencyAndId();
        tripAgencyAndId.setId(tripId);
        tripAgencyAndId.setAgencyId(agency.getId());
        Trip trip = new Trip();
        trip.setId(tripAgencyAndId);

        AgencyAndId serviceAgencyAndId = new AgencyAndId();
        Set<DayType> dayTypes = serviceJourney.getDayTypes().getDayTypeRef().stream().map(jaxbElement -> jaxbElement.getValue().getRef()).map(netexTimetableEntitiesIndex.getDayTypeIndex()::get).collect(Collectors.toSet());
        serviceAgencyAndId.setId(gtfsServiceRepository.getService(dayTypes).getId());
        serviceAgencyAndId.setAgencyId(agency.getId());
        trip.setServiceId(serviceAgencyAndId);

        trip.setRoute(route);


        return trip;

    }

}
