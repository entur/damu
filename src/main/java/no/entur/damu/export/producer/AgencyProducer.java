package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.Agency;
import org.rutebanken.netex.model.Authority;

public interface AgencyProducer {
    Agency produce(Authority authority);
}
