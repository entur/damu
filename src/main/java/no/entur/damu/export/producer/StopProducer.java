package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.Stop;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;


/**
 * Produce a GTFS Stop.
 */
public interface StopProducer {

    /**
     * Produce a GTFS Stop from a NeTEx StopPlace
     * @param stopPlace a NeTEx StopPlace
     * @return a GTFS stop
     */
    Stop produceStopFromStopPlace(StopPlace stopPlace);

    /**
     * Produce a GTFS Stop from a NeTEx Quay
     * @param quay a NeTEx Quay
     * @return a GTFS stop
     */
    Stop produceStopFromQuay(Quay quay);
}
