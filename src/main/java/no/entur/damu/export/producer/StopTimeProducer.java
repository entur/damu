package no.entur.damu.export.producer;

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

import static no.entur.damu.export.util.GtfsUtil.toGtfsTime;

public class StopTimeProducer {

    private final NetexEntitiesIndex netexTimetableEntitiesIndex;
    private final GtfsMutableDao gtfsDao;


    public StopTimeProducer(NetexEntitiesIndex netexTimetableEntitiesIndex, GtfsMutableDao gtfsDao) {
        this.netexTimetableEntitiesIndex = netexTimetableEntitiesIndex;
        this.gtfsDao = gtfsDao;
    }

    public StopTime produce(TimetabledPassingTime timetabledPassingTime, JourneyPattern journeyPattern, Trip trip, boolean multipleDestinationDisplays) {
        StopTime stopTime = new StopTime();
        stopTime.setTrip(trip);

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

        if (timetabledPassingTime.getArrivalTime() != null) {
            int arrivalTime = toGtfsTime(timetabledPassingTime.getArrivalTime());
            int offSetDay = timetabledPassingTime.getArrivalDayOffset() == null ? 0 : timetabledPassingTime.getArrivalDayOffset().intValueExact();
            stopTime.setArrivalTime(arrivalTime + offSetDay * 60 * 60 * 24);
        }

        if (timetabledPassingTime.getDepartureTime() != null) {
            int departureTime = toGtfsTime(timetabledPassingTime.getDepartureTime());
            int offSetDay = timetabledPassingTime.getDepartureDayOffset() == null ? 0 : timetabledPassingTime.getDepartureDayOffset().intValueExact();
            stopTime.setDepartureTime(departureTime + offSetDay * 60 * 60 * 24);
        }

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

        return stopTime;
    }

}
