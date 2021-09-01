package no.entur.damu.producer;

import no.entur.damu.util.GtfsUtil;
import org.onebusaway.gtfs.model.Agency;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.Operator;

public class AgencyProducer {

    private final String timeZone;

    public AgencyProducer(String timeZone) {
        this.timeZone = timeZone;
    }

    public Agency produce(Authority authority) {
        Agency agency = new Agency();
        agency.setId(GtfsUtil.toGtfsId(authority.getId(), null, true));
        agency.setName(authority.getName().getValue());
        agency.setUrl(authority.getContactDetails().getUrl());
        agency.setPhone(authority.getContactDetails().getPhone());
        agency.setTimezone(timeZone);
        return agency;
    }

    public Agency produce(Operator operator) {
        Agency agency = new Agency();
        agency.setId(GtfsUtil.toGtfsId(operator.getId(), null, true));
        agency.setName(operator.getName().getValue());

        if(operator.getContactDetails() != null) {
            agency.setUrl(operator.getContactDetails().getUrl());
            agency.setPhone(operator.getContactDetails().getPhone());
        } else {
            agency.setUrl("-");
        }
        agency.setTimezone(timeZone);
        return agency;

    }
}
