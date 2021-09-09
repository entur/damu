package no.entur.damu.export.producer;

import no.entur.damu.export.util.GtfsUtil;
import org.onebusaway.gtfs.model.Agency;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgencyProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgencyProducer.class);

    private final String timeZone;

    public AgencyProducer(String timeZone) {
        this.timeZone = timeZone;
    }

    public Agency produce(Authority authority) {
        Agency agency = new Agency();
        agency.setId(GtfsUtil.toGtfsId(authority.getId(), null, true));
        agency.setName(authority.getName().getValue());
        if (authority.getContactDetails() != null) {
            if (authority.getContactDetails().getUrl() != null) {
                agency.setUrl(authority.getContactDetails().getUrl());
            } else {
                LOGGER.warn("Missing URL for authority {}", authority.getId());
                agency.setUrl("-");
            }
            agency.setPhone(authority.getContactDetails().getPhone());
        } else {
            LOGGER.warn("Missing Contact details for authority {}", authority.getId());
            agency.setUrl("-");
        }
        agency.setTimezone(timeZone);
        return agency;
    }

    public Agency produce(Operator operator) {
        Agency agency = new Agency();
        agency.setId(GtfsUtil.toGtfsId(operator.getId(), null, true));
        agency.setName(operator.getName().getValue());

        if (operator.getContactDetails() != null) {
            if (operator.getContactDetails().getUrl() != null) {
                agency.setUrl(operator.getContactDetails().getUrl());
            } else {
                LOGGER.warn("Missing URL for operator {}", operator.getId());
                agency.setUrl("-");
            }
            agency.setPhone(operator.getContactDetails().getPhone());
        } else {
            LOGGER.warn("Missing Contact details for operator {}", operator.getId());
            agency.setUrl("-");
        }
        agency.setTimezone(timeZone);
        return agency;

    }
}
