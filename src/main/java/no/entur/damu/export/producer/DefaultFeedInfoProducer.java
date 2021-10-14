package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.FeedInfo;

/**
 * Return an undefined (null) feed info by default.
 */
public class DefaultFeedInfoProducer implements FeedInfoProducer {

    /**
     * Return null
     * @return null
     */
    @Override
    public FeedInfo produceFeedInfo() {
        return null;
    }
}
