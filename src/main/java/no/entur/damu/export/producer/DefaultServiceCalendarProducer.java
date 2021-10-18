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
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

import java.util.Collection;

public class DefaultServiceCalendarProducer implements ServiceCalendarProducer {

    private final Agency agency;

    public DefaultServiceCalendarProducer(GtfsDatasetRepository gtfsDatasetRepository) {
        this.agency = gtfsDatasetRepository.getDefaultAgency();
    }

    @Override
    public ServiceCalendar produce(String serviceId, ServiceDate startDate, ServiceDate endDate, Collection<DayOfWeekEnumeration> daysOfWeeks) {
        ServiceCalendar serviceCalendar = new ServiceCalendar();

        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setId(serviceId);
        agencyAndId.setAgencyId(agency.getId());
        serviceCalendar.setServiceId(agencyAndId);
        serviceCalendar.setStartDate(startDate);
        serviceCalendar.setEndDate(endDate);

        if (daysOfWeeks.isEmpty()) {
            serviceCalendar.setMonday(1);
            serviceCalendar.setTuesday(1);
            serviceCalendar.setWednesday(1);
            serviceCalendar.setThursday(1);
            serviceCalendar.setFriday(1);
            serviceCalendar.setSaturday(1);
            serviceCalendar.setSunday(1);
        } else {
            serviceCalendar.setMonday(daysOfWeeks.contains(DayOfWeekEnumeration.MONDAY) ? 1 : 0);
            serviceCalendar.setTuesday(daysOfWeeks.contains(DayOfWeekEnumeration.TUESDAY) ? 1 : 0);
            serviceCalendar.setWednesday(daysOfWeeks.contains(DayOfWeekEnumeration.WEDNESDAY) ? 1 : 0);
            serviceCalendar.setThursday(daysOfWeeks.contains(DayOfWeekEnumeration.THURSDAY) ? 1 : 0);
            serviceCalendar.setFriday(daysOfWeeks.contains(DayOfWeekEnumeration.FRIDAY) ? 1 : 0);
            serviceCalendar.setSaturday(daysOfWeeks.contains(DayOfWeekEnumeration.SATURDAY) ? 1 : 0);
            serviceCalendar.setSunday(daysOfWeeks.contains(DayOfWeekEnumeration.SUNDAY) ? 1 : 0);

            if (daysOfWeeks.contains(DayOfWeekEnumeration.WEEKDAYS)) {
                serviceCalendar.setMonday(1);
                serviceCalendar.setTuesday(1);
                serviceCalendar.setWednesday(1);
                serviceCalendar.setThursday(1);
                serviceCalendar.setFriday(1);
            }

            if (daysOfWeeks.contains(DayOfWeekEnumeration.WEEKEND)) {
                serviceCalendar.setSaturday(1);
                serviceCalendar.setSunday(1);
            }
        }

        return serviceCalendar;
    }
}
