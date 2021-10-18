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

package no.entur.damu.export.model;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * A GTFS service calendar period defined by an interval [startDate, endDate] and a set of days of week for which the service is running.
 * If no days of week are specified then the service runs every day of the week.
 */
public class ServiceCalendarPeriod {

    private final ServiceDate startDate;
    private final ServiceDate endDate;

    private List<DayOfWeekEnumeration> daysOfWeek;

    public ServiceCalendarPeriod(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        startDate = new ServiceDate(toGtfsDate(startDateTime));
        endDate = new ServiceDate(toGtfsDate(endDateTime));
    }


    public ServiceDate getStartDate() {
        return startDate;
    }

    public ServiceDate getEndDate() {
        return endDate;
    }

    public List<DayOfWeekEnumeration> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<DayOfWeekEnumeration> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }


    private static Date toGtfsDate(LocalDateTime netexDate) {
        return Date.from(netexDate.atZone(ZoneId.systemDefault()).toInstant());
    }
}
