package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.Stop;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;

public interface StopProducer {
    Stop produceStopFromStopPlace(StopPlace stopPlace);

    Stop produceStopFromQuay(Quay quay);
}
