package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;


/**
 * Produce a GTFS Trip.
 */
public interface TripProducer {

    /**
     * Return a GTFS Trip corresponding to the ServiceJourney or null if the ServiceJourney cannot be converted into a valid GTFS trip.
     *
     * @param serviceJourney            the NeTEx service journey
     * @param netexRoute                the NeTEx route
     * @param gtfsRoute                 the GTFS route
     * @param shapeId                   the optional shape id
     * @param initialDestinationDisplay the initial destination display.
     * @return a GTFS Trip corresponding to the ServiceJourney or null if the ServiceJourney cannot be converted into a valid GTFS trip.
     */
    Trip produce(ServiceJourney serviceJourney, Route netexRoute, org.onebusaway.gtfs.model.Route gtfsRoute, AgencyAndId shapeId, DestinationDisplay initialDestinationDisplay);
}
