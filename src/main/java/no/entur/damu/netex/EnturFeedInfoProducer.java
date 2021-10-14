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
