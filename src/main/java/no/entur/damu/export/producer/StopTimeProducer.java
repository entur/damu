package no.entur.damu.export.producer;

import no.entur.damu.export.model.GtfsShape;
import no.entur.damu.export.util.DestinationDisplayUtil;
import no.entur.damu.export.util.StopUtil;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.entur.damu.export.util.GtfsUtil.toGtfsTimeWithDayOffset;

public class StopTimeProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopTimeProducer.class);

    private static final int PICKUP_AND_DROP_OFF_TYPE_NOT_AVAILABLE = 1;

    private final NetexEntitiesIndex netexTimetableEntitiesIndex;
    private final GtfsMutableDao gtfsDao;


    public StopTimeProducer(NetexEntitiesIndex netexTimetableEntitiesIndex, GtfsMutableDao gtfsDao) {
        this.netexTimetableEntitiesIndex = netexTimetableEntitiesIndex;
        this.gtfsDao = gtfsDao;
    }

    public StopTime produce(TimetabledPassingTime timetabledPassingTime, JourneyPattern journeyPattern, Trip trip, GtfsShape gtfsShape, boolean multipleDestinationDisplays) {
        StopTime stopTime = new StopTime();

        // trip
        stopTime.setTrip(trip);

        // stop
        String pointInJourneyPatternRef = timetabledPassingTime.getPointInJourneyPatternRef().getValue().getRef();
        StopPointInJourneyPattern stopPointInSequence = (StopPointInJourneyPattern) journeyPattern.getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                .stream()
                .filter(stopPointInJourneyPattern -> stopPointInJourneyPattern.getId().equals(pointInJourneyPatternRef))
                .findFirst()
                .orElseThrow();
        int stopSequence = stopPointInSequence.getOrder().intValueExact();
        stopTime.setStopSequence(stopSequence);
        String scheduledStopPointId = stopPointInSequence.getScheduledStopPointRef().getValue().getRef();
        Stop stop = StopUtil.getGtfsStopFromScheduledStopPointId(scheduledStopPointId, netexTimetableEntitiesIndex, gtfsDao);
        stopTime.setStop(stop);

        // arrival time
        if (timetabledPassingTime.getArrivalTime() != null) {
            int dayOffset = timetabledPassingTime.getArrivalDayOffset() == null ? 0 : timetabledPassingTime.getArrivalDayOffset().intValueExact();
            stopTime.setArrivalTime(toGtfsTimeWithDayOffset(timetabledPassingTime.getArrivalTime(), dayOffset));

            if (timetabledPassingTime.getDepartureTime() == null) {
                stopTime.setDepartureTime(stopTime.getArrivalTime());
            }
        }

        // departure time
        if (timetabledPassingTime.getDepartureTime() != null) {
            int dayOffset = timetabledPassingTime.getDepartureDayOffset() == null ? 0 : timetabledPassingTime.getDepartureDayOffset().intValueExact();
            stopTime.setDepartureTime(toGtfsTimeWithDayOffset(timetabledPassingTime.getDepartureTime(), dayOffset));

            if (timetabledPassingTime.getArrivalTime() == null) {
                stopTime.setArrivalTime(stopTime.getDepartureTime());
            }

        }

        // destination display = stop head sign
        if (multipleDestinationDisplays && stopPointInSequence.getDestinationDisplayRef() != null) {
            DestinationDisplay destinationDisplay = netexTimetableEntitiesIndex.getDestinationDisplayIndex().get(stopPointInSequence.getDestinationDisplayRef().getRef());
            String stopHeadSign = DestinationDisplayUtil.getFrontTextWithComputedVias(destinationDisplay, netexTimetableEntitiesIndex);
            if (trip.getTripHeadsign() != null) {
                if (!trip.getTripHeadsign().equals(stopHeadSign)) {
                    stopTime.setStopHeadsign(stopHeadSign);
                }
            } else {
                stopTime.setStopHeadsign(stopHeadSign);
            }
        }

        // boarding = pickup
        if (Boolean.FALSE.equals(stopPointInSequence.isForBoarding())) {
            stopTime.setPickupType(PICKUP_AND_DROP_OFF_TYPE_NOT_AVAILABLE);
        }

        // alighting = drop off
        if (Boolean.FALSE.equals(stopPointInSequence.isForAlighting())) {
            stopTime.setDropOffType(PICKUP_AND_DROP_OFF_TYPE_NOT_AVAILABLE);
        }

        // distance travelled
        if (trip.getShapeId() == null) {
            LOGGER.debug("skipping distance travelled for trip {}", trip.getId());
        } else if (stopSequence >= 2) {
            stopTime.setShapeDistTraveled(gtfsShape.getDistanceTravelledToServiceLink(stopSequence - 2));
        }


        return stopTime;
    }
}
