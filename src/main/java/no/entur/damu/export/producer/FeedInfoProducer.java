package no.entur.damu.export.producer;

import org.onebusaway.gtfs.model.FeedInfo;

/**
 * Produce an optional GTFS feed info or null if undefined.
 */
public interface FeedInfoProducer {

    /**
     * Produce an optional GTFS feed info or null if undefined.
     *
     * @return an optional GTFS feed info or null if undefined.
     */
    FeedInfo produceFeedInfo();
}
