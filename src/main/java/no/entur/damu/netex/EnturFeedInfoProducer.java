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

import no.entur.damu.export.producer.FeedInfoProducer;
import org.onebusaway.gtfs.model.FeedInfo;

public class EnturFeedInfoProducer implements FeedInfoProducer {

    @Override
    public FeedInfo produceFeedInfo() {

        FeedInfo feedInfo = new FeedInfo();
        feedInfo.setId("ENTUR");
        feedInfo.setPublisherName("Entur");
        feedInfo.setPublisherUrl("https://www.entur.org");
        feedInfo.setLang("no");
        return feedInfo;
    }
}
