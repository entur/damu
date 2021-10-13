package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;

public interface TripProducer {
    Trip produce(ServiceJourney serviceJourney, JourneyPattern journeyPattern, Route netexRoute, org.onebusaway.gtfs.model.Route gtfsRoute, AgencyAndId shapeId, DestinationDisplay startDestinationDisplay);
}
