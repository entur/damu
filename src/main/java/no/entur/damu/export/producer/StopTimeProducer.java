package no.entur.damu.export.producer;

import no.entur.damu.export.model.GtfsShape;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;


/**
 * Produce a GTFS Stop Time.
 */
public interface StopTimeProducer {
    StopTime produce(TimetabledPassingTime timetabledPassingTime, JourneyPattern journeyPattern, Trip trip, GtfsShape gtfsShape, String currentHeadSign);
}
