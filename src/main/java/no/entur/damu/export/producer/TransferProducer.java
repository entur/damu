package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.Transfer;
import org.rutebanken.netex.model.ServiceJourneyInterchange;


/**
 * Produce a GTFS Transfer
 */
public interface TransferProducer {
    /**
     * Produce a GTFS Transfer from a NeTEx Service Journey Interchange.
     * @param serviceJourneyInterchange the NeTEx Service Journey Interchange.
     * @return the GTFS Transfer.
     */
    Transfer produce(ServiceJourneyInterchange serviceJourneyInterchange);
}
