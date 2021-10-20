/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *  *
 *
 */

package no.entur.damu.netex;

import org.entur.netex.gtfs.export.exception.MissingAuthorityUrlException;
import org.entur.netex.gtfs.export.producer.DefaultAgencyProducer;
import org.entur.netex.gtfs.export.repository.NetexDatasetRepository;
import org.onebusaway.gtfs.model.Agency;
import org.rutebanken.netex.model.Authority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Custom agency producer that adds missing agency URLs.
 */
public class EnturAgencyProducer extends DefaultAgencyProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnturAgencyProducer.class);


    private static final Map<String, String> URL_BY_PROVIDER = Map.of(

            "AKT", "https://www.akt.no/",
            "TID", "https://flybussen.no/",
            "GOA", "https://www.go-aheadnordic.no/",
            "INN", "https:/www.innlandstrafikk.no/",
            "VKT", "https://www.vkt.no/",

            "RUT", "https://ruter.no/",
            "UNI", "https://www.unibuss.no/",
            "TAG", "https://www.vy.no/",
            "VYG", "https://www.vy.no/",
            "MOR", "https://www.frammr.no/"
    );

    private final String codespace;

    public EnturAgencyProducer(NetexDatasetRepository netexDatasetRepository, String codespace) {
        super(netexDatasetRepository);
        this.codespace = codespace;
    }

    @Override
    public Agency produce(Authority authority) {
        Agency agency = super.produce(authority);
        if (agency.getUrl() == null) {
            String knownUrl = URL_BY_PROVIDER.get(codespace);
            if (knownUrl != null) {
                LOGGER.warn("Adding missing URL {} for codespace {}", knownUrl, codespace);
                agency.setUrl(knownUrl);
            } else {
                throw new MissingAuthorityUrlException("URL not found for agency " + agency.getId());
            }
        }
        return agency;
    }
}
