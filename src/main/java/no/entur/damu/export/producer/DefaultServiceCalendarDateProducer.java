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

package no.entur.damu.export.producer;

import no.entur.damu.export.repository.GtfsDatasetRepository;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class DefaultServiceCalendarDateProducer implements  ServiceCalendarDateProducer{

    private final Agency agency;

    public DefaultServiceCalendarDateProducer(GtfsDatasetRepository gtfsDatasetRepository) {
        this.agency = gtfsDatasetRepository.getDefaultAgency();
    }

    @Override
    public ServiceCalendarDate produce(String serviceId, LocalDateTime date, boolean isAvailable) {
        ServiceCalendarDate serviceCalendarDate = new ServiceCalendarDate();
        AgencyAndId serviceCalendarDateAgencyAndId = new AgencyAndId();
        serviceCalendarDateAgencyAndId.setId(serviceId);
        serviceCalendarDateAgencyAndId.setAgencyId(agency.getId());
        serviceCalendarDate.setServiceId(serviceCalendarDateAgencyAndId);
        serviceCalendarDate.setDate(new ServiceDate(toGtfsDate(date)));
        serviceCalendarDate.setExceptionType(isAvailable ? 1 : 2);

        return serviceCalendarDate;
    }

    private static Date toGtfsDate(LocalDateTime netexDate) {
        return Date.from(netexDate.atZone(ZoneId.systemDefault()).toInstant());
    }


}
