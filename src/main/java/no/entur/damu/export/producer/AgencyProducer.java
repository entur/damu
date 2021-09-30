package no.entur.damu.export.producer;

import no.entur.damu.export.util.GtfsUtil;
import org.onebusaway.gtfs.model.Agency;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.ContactStructure;
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

        // agency name
        agency.setName(authority.getName().getValue());

        // agency URL and phone
        ContactStructure contactDetails = authority.getContactDetails();
        if (contactDetails != null) {
            if (contactDetails.getUrl() != null && !contactDetails.getUrl().isBlank()) {
                agency.setUrl(contactDetails.getUrl());
            } else {
                LOGGER.warn("Missing URL for authority {}", authority.getId());
                agency.setUrl("-");
            }
            agency.setPhone(contactDetails.getPhone());
        } else {
            LOGGER.warn("Missing Contact details for authority {}", authority.getId());
            agency.setUrl("-");
        }

        // agency timezone
        agency.setTimezone(timeZone);

        return agency;
    }
}
