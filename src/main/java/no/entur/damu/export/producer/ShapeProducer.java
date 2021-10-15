package no.entur.damu.export.producer;

import no.entur.damu.export.model.GtfsShape;
import org.rutebanken.netex.model.JourneyPattern;


/**
 * Produce a GTFS Shape.
 */
public interface ShapeProducer {
    /**
     * Produce a GTFS shape for a given journey pattern.
     * @param journeyPattern a NeTEx journey pattern.
     * @return a GTFS shape.
     */
    GtfsShape produce(JourneyPattern journeyPattern);
}
