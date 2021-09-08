package no.entur.damu.producer;

import no.entur.damu.model.RouteTypeEnum;
import no.entur.damu.model.TransportModeNameEnum;
import no.entur.damu.stop.StopAreaRepository;
import no.entur.damu.util.NetexParserUtils;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.VehicleModeEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopProducer.class);
    private static final int WHEELCHAIR_BOARDING_TRUE = 1;


    private final Agency agency;
    private final StopAreaRepository stopAreaRepository;

    public StopProducer(StopAreaRepository stopAreaRepository) {
        this.agency = createEnturAgency();
        this.stopAreaRepository = stopAreaRepository;
    }

    /**
     * Return an agency representing Entur.
     * The OneBusAway API requires an agency linked to stops, even if it does not appear in the GTFS export
     *
     * @return an agency representing Entur.
     */
    private static Agency createEnturAgency() {
        Agency enturAgency = new Agency();
        enturAgency.setId("ENT");
        enturAgency.setUrl("https://www.entur.org");
        enturAgency.setName("Entur");
        return enturAgency;
    }


    public Stop produceStopFromStopPlace(StopPlace stopPlace) {
        Stop stop = new Stop();
        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setAgencyId(agency.getId());
        agencyAndId.setId(stopPlace.getId());
        stop.setId(agencyAndId);
        stop.setLocationType(Stop.LOCATION_TYPE_STOP);

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

        stop.setLon(stopPlace.getCentroid().getLocation().getLongitude().doubleValue());
        stop.setLat(stopPlace.getCentroid().getLocation().getLatitude().doubleValue());


        if (stopPlace.getAccessibilityAssessment() != null
                && "true".equals(stopPlace.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().value())) {
            stop.setWheelchairBoarding(WHEELCHAIR_BOARDING_TRUE);
        }

        VehicleModeEnumeration netexTransportMode = stopPlace.getTransportMode();
        if (netexTransportMode != null) {
            TransportModeNameEnum transportMode = NetexParserUtils.toTransportModeNameEnum(netexTransportMode.value());
            stop.setVehicleType(RouteTypeEnum.from(transportMode, null).getValue());
        } else {
            LOGGER.warn("Missing transport mode for stop place {}", stop.getId());
        }

        return stop;
    }


    public Stop produceStopFromQuay(Quay quay) {
        Stop stop = new Stop();
        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setAgencyId(agency.getId());
        agencyAndId.setId(quay.getId());
        stop.setId(agencyAndId);
        stop.setLocationType(Stop.LOCATION_TYPE_STOP);

        if (quay.getPrivateCode() != null) {
            stop.setPlatformCode(quay.getPrivateCode().getValue());
        }

        StopPlace stopPlace = stopAreaRepository.getStopPlaceByQuayId(quay.getId());
        stop.setParentStation(stopPlace.getId());

        VehicleModeEnumeration netexTransportMode = stopPlace.getTransportMode();
        if (netexTransportMode != null) {
            TransportModeNameEnum transportMode = NetexParserUtils.toTransportModeNameEnum(netexTransportMode.value());
            stop.setVehicleType(RouteTypeEnum.from(transportMode, null).getValue());
        } else {
            LOGGER.warn("Missing transport mode for quay {}", stop.getId());
        }

        stop.setName(stopPlace.getName().getValue());
        if (quay.getDescription() != null) {
            stop.setDesc(quay.getDescription().getValue());
        }

        stop.setLon(quay.getCentroid().getLocation().getLongitude().doubleValue());
        stop.setLat(quay.getCentroid().getLocation().getLatitude().doubleValue());


        if (quay.getAccessibilityAssessment() != null
                && "true".equals(quay.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().value())) {
            stop.setWheelchairBoarding(WHEELCHAIR_BOARDING_TRUE);
        }


        return stop;
    }

}
