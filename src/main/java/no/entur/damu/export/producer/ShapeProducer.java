package no.entur.damu.export.producer;

import no.entur.damu.export.model.GtfsShape;
import org.rutebanken.netex.model.JourneyPattern;

public interface ShapeProducer {
    GtfsShape produce(JourneyPattern journeyPattern);
}
