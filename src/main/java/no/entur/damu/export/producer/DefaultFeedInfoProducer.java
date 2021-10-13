package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.FeedInfo;

public class DefaultFeedInfoProducer implements FeedInfoProducer {

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
