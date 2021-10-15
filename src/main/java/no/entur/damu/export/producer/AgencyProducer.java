package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.Agency;
import org.rutebanken.netex.model.Authority;

/**
 * Produce a GTFS Agency
 */
public interface AgencyProducer {

    /**
     * Produce a GTFS agency from a NeTEx authority.
     * @param authority a NeTEx authority
     * @return the GTS agency.
     */
    Agency produce(Authority authority);
}
