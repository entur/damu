package no.entur.damu.netex;

import no.entur.damu.export.producer.DefaultAgencyProducer;
import no.entur.damu.export.repository.NetexDatasetRepository;
import org.onebusaway.gtfs.model.Agency;
import org.rutebanken.netex.model.Authority;

public class DamuAgencyProducer extends DefaultAgencyProducer {
    public DamuAgencyProducer(NetexDatasetRepository netexDatasetRepository) {
        super(netexDatasetRepository);
    }

    @Override
    public Agency produce(Authority authority) {
        Agency agency = super.produce(authority);
        // TODO temporarily setting the URL to pass validation
        if (agency.getUrl() == null) {
            agency.setUrl("https://");
        }
        return agency;
    }
}
