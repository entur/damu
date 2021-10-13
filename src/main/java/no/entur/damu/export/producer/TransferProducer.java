package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.Transfer;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

public interface TransferProducer {
    Transfer produce(ServiceJourneyInterchange serviceJourneyInterchange);
}
