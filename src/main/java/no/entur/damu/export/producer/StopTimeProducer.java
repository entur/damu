package no.entur.damu.export.producer;

import org.entur.netex.index.api.NetexEntitiesIndex;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;

import static no.entur.damu.export.util.GtfsUtil.toGtfsTime;

public class StopTimeProducer {

    private static final String ENTUR_AGENCY_ID = "ENT";
    private final NetexEntitiesIndex netexTimetableEntitiesIndex;
    private final GtfsMutableDao gtfsDao;


    public StopTimeProducer(NetexEntitiesIndex netexTimetableEntitiesIndex, GtfsMutableDao gtfsDao) {
        this.netexTimetableEntitiesIndex = netexTimetableEntitiesIndex;
        this.gtfsDao = gtfsDao;
    }


    public StopTime produce(TimetabledPassingTime timetabledPassingTime, JourneyPattern journeyPattern, Trip trip) {
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

        String stopId = netexTimetableEntitiesIndex.getQuayIdByStopPointRefIndex().get(stopPointInSequence.getScheduledStopPointRef().getValue().getRef());

        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setId(stopId);
        agencyAndId.setAgencyId(ENTUR_AGENCY_ID);
        Stop stop = gtfsDao.getStopForId(agencyAndId);
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

        return stopTime;
    }

}
