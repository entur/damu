package no.entur.damu.producer;

import no.entur.damu.model.RouteTypeEnum;
import no.entur.damu.model.TransportModeNameEnum;
import no.entur.damu.util.NetexParserUtils;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;

import java.util.Map;

public class StopProducer {

    private static final int WHEELCHAIR_BOARDING_TRUE = 1;


    private final Agency agency;
    private final Map<String, StopPlace> stopPlaces;
    private final String timezone;

    public StopProducer(Agency agency, Map<String, StopPlace> stopPlaces, String timezone) {
        this.agency = agency;
        this.stopPlaces = stopPlaces;
        this.timezone = timezone;
    }


    public Stop produceStopFromStopPlace(StopPlace stopPlace) {
        Stop stop = new Stop();
        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setAgencyId(agency.getId());
        agencyAndId.setId(stopPlace.getId());
        stop.setId(agencyAndId);
        stop.setLocationType(Stop.LOCATION_TYPE_STOP);
        stop.setTimezone(timezone);

        if (stopPlace.getPrivateCode() != null) {
            stop.setPlatformCode(stopPlace.getPrivateCode().getValue());
        }

        if (stopPlace.getParentSiteRef() != null) {
            stop.setParentStation(stopPlace.getParentSiteRef().getRef());
        }

        if (stopPlace.getName() != null) {
            stop.setName(stopPlace.getName().getValue());

        }
        if (stopPlace.getDescription() != null) {
            stop.setDesc(stopPlace.getDescription().getValue());
        }
        //stop.setCode();

        stop.setLon(stopPlace.getCentroid().getLocation().getLongitude().doubleValue());
        stop.setLat(stopPlace.getCentroid().getLocation().getLatitude().doubleValue());


        if (stopPlace.getAccessibilityAssessment() != null
                && stopPlace.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().value().equals("true")) {
            stop.setWheelchairBoarding(WHEELCHAIR_BOARDING_TRUE);
        }

        TransportModeNameEnum transportMode = NetexParserUtils.toTransportModeNameEnum(stopPlace.getTransportMode().value());
        stop.setVehicleType(RouteTypeEnum.from(transportMode, null).getValue());


        return stop;
    }


    public Stop produceStopFromQuay(Quay quay) {
        Stop stop = new Stop();
        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setAgencyId(agency.getId());
        agencyAndId.setId(quay.getId());
        stop.setId(agencyAndId);
        stop.setLocationType(Stop.LOCATION_TYPE_STOP);
        stop.setTimezone(timezone);

        if (quay.getPrivateCode() != null) {
            stop.setPlatformCode(quay.getPrivateCode().getValue());
        }

        StopPlace stopPlace = stopPlaces.get(quay.getId());
        stop.setParentStation(stopPlace.getId());

        TransportModeNameEnum transportMode = NetexParserUtils.toTransportModeNameEnum(stopPlace.getTransportMode().value());
        stop.setVehicleType(RouteTypeEnum.from(transportMode, null).getValue());

        stop.setName(stopPlace.getName().getValue());
        if (quay.getDescription() != null) {
            stop.setDesc(quay.getDescription().getValue());
        }
        //stop.setCode();

        stop.setLon(quay.getCentroid().getLocation().getLongitude().doubleValue());
        stop.setLat(quay.getCentroid().getLocation().getLatitude().doubleValue());


        if (quay.getAccessibilityAssessment() != null
                && quay.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().value().equals("true")) {
            stop.setWheelchairBoarding(WHEELCHAIR_BOARDING_TRUE);
        }


        return stop;
    }

}
