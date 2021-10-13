package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.Route;
import org.rutebanken.netex.model.Line;

public interface RouteProducer {
    Route produce(Line line);
}
