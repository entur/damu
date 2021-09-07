package no.entur.damu.producer;

import org.onebusaway.gtfs.model.FeedInfo;

public class FeedInfoProducer {

    public FeedInfo produceFeedInfo() {

        FeedInfo feedInfo = new FeedInfo();
        feedInfo.setId("ENTUR");
        feedInfo.setPublisherName("Entur");
        feedInfo.setPublisherUrl("https://www.entur.org");
        feedInfo.setLang("no");
        return feedInfo;
    }
}
