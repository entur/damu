package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.Route;
import org.rutebanken.netex.model.Line;


/**
 * Produce a GTFS Route.
 */
public interface RouteProducer {
    /**
     * Produce a GTFS Route for a given NeTEx Line.
     * @param line the NeTEx line.
     * @return the GTFS Route
     */
    Route produce(Line line);
}
