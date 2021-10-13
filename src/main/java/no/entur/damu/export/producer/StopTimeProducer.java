package no.entur.damu.export.producer;

import no.entur.damu.export.model.GtfsShape;
import no.entur.damu.export.repository.GtfsDatasetRepository;
import no.entur.damu.export.repository.NetexDatasetRepository;
import no.entur.damu.export.util.DestinationDisplayUtil;
import no.entur.damu.export.util.StopUtil;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
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
    private static final int PICKUP_AND_DROP_OFF_TYPE_MUST_COORDINATE_WITH_DRIVER = 3;

    private final NetexDatasetRepository netexDatasetRepository;
    private final GtfsDatasetRepository gtfsDatasetRepository;


    public StopTimeProducer(NetexDatasetRepository netexDatasetRepository, GtfsDatasetRepository gtfsDatasetRepository) {
        this.netexDatasetRepository = netexDatasetRepository;
        this.gtfsDatasetRepository = gtfsDatasetRepository;
    }

    public StopTime produce(TimetabledPassingTime timetabledPassingTime, JourneyPattern journeyPattern, Trip trip, GtfsShape gtfsShape, String currentHeadSign) {
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
        Stop stop = StopUtil.getGtfsStopFromScheduledStopPointId(scheduledStopPointId, netexDatasetRepository, gtfsDatasetRepository);
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
        // the head sign is by default the destination display set on the current stop
        // it can be ignored if it is the same as the trip head sign
        String stopHeadSignOnCurrentStop = null;
        if (stopPointInSequence.getDestinationDisplayRef() != null) {
            DestinationDisplay destinationDisplay = netexDatasetRepository.getDestinationDisplayById(stopPointInSequence.getDestinationDisplayRef().getRef());
            stopHeadSignOnCurrentStop = DestinationDisplayUtil.getFrontTextWithComputedVias(destinationDisplay, netexDatasetRepository);
            if(stopHeadSignOnCurrentStop != null && stopHeadSignOnCurrentStop.equals(trip.getTripHeadsign())) {
                stopHeadSignOnCurrentStop = null;
            }
        }
        // otherwise the head sign from the previous stop is used
        // in GTFS the head sign must be explicitly set from the first stop where the head sign has changed to the last stop the change applies.
        if (stopHeadSignOnCurrentStop == null) {
            stopHeadSignOnCurrentStop = currentHeadSign;
        }
        stopTime.setStopHeadsign(stopHeadSignOnCurrentStop);

        // boarding = pickup
        if (Boolean.FALSE.equals(stopPointInSequence.isForBoarding())) {
            stopTime.setPickupType(PICKUP_AND_DROP_OFF_TYPE_NOT_AVAILABLE);
        }

        // alighting = drop off
        if (Boolean.FALSE.equals(stopPointInSequence.isForAlighting())) {
            stopTime.setDropOffType(PICKUP_AND_DROP_OFF_TYPE_NOT_AVAILABLE);
        }

        // pickup and stop on request override the values set in isForBoarding and isForAlighting
        if (Boolean.TRUE.equals(stopPointInSequence.isRequestStop())) {
            stopTime.setPickupType(PICKUP_AND_DROP_OFF_TYPE_MUST_COORDINATE_WITH_DRIVER);
            stopTime.setDropOffType(PICKUP_AND_DROP_OFF_TYPE_MUST_COORDINATE_WITH_DRIVER);
        }

        // distance travelled
        if (trip.getShapeId() == null) {
            LOGGER.debug("skipping distance travelled for trip {}", trip.getId());
        } else {
            stopTime.setShapeDistTraveled(gtfsShape.getDistanceTravelledToStop(stopSequence));
        }


        return stopTime;
    }
}
