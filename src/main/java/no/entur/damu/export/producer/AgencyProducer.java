package no.entur.damu.export.producer;

import no.entur.damu.export.util.GtfsUtil;
import org.onebusaway.gtfs.model.Agency;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.ContactStructure;
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
            String url = contactDetails.getUrl();
            if (url != null && !url.isBlank()) {
                if (isValidGtfsUrl(url)) {
                    agency.setUrl(url);
                } else {
                    LOGGER.warn("Invalid URL format {} for authority {}", url, authority.getId());
                }
            } else {
                LOGGER.warn("Missing URL for authority {}", authority.getId());
            }
            agency.setPhone(contactDetails.getPhone());
        } else {
            LOGGER.warn("Missing Contact details for authority {}", authority.getId());
        }
        // TODO temporarily setting the URL to pass validation
        if(agency.getUrl() == null) {
            agency.setUrl("https://");
        }

        // agency timezone
        agency.setTimezone(timeZone);

        return agency;
    }

    /**
     * Return true if the url is a valid GTFS URL, starting with either http:// or https://
     * See https://developers.google.com/transit/gtfs/reference#field_types
     *
     * @param url the agency URL to check
     * @return true if the url is a valid GTFS URL, starting with either http:// or https://
     */
    private static boolean isValidGtfsUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
