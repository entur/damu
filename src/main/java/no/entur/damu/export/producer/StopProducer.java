package no.entur.damu.export.producer;

import no.entur.damu.export.model.RouteTypeEnum;
import no.entur.damu.export.model.TransportModeNameEnum;
import no.entur.damu.export.repository.GtfsDatasetRepository;
import no.entur.damu.export.stop.StopAreaRepository;
import no.entur.damu.export.util.NetexParserUtils;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.VehicleModeEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Produce a GTFS stop from a NeTEX StopPlace or a NeTEx Quay.
 * Supported GTFS extension: vehicle_type (see https://developers.google.com/transit/gtfs/reference/gtfs-extensions/#stops.txt)
 */
public class StopProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopProducer.class);
    private static final int WHEELCHAIR_BOARDING_TRUE = 1;
    private static final int WHEELCHAIR_BOARDING_FALSE = 2;

    private final Agency defaultAgency;
    private final StopAreaRepository stopAreaRepository;

    public StopProducer(StopAreaRepository stopAreaRepository, GtfsDatasetRepository gtfsDatasetRepository) {
        this.defaultAgency = gtfsDatasetRepository.getDefaultAgency();
        this.stopAreaRepository = stopAreaRepository;
    }


    /**
     * Produce a GTFS stop from a NeTEx StopPlace
     * The GTFS parent station is not set and the NeTEX parent stop place is ignored.
     * The GTFS location type is set to Station.
     * The GTFS description is based on the NeTEx description only if it is different from the NeTEx name, it is ignored otherwise.
     *
     * @param stopPlace a NeTEx stop place
     * @return a GTFS stop.
     */
    public Stop produceStopFromStopPlace(StopPlace stopPlace) {
        Stop stop = new Stop();
        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setAgencyId(defaultAgency.getId());
        agencyAndId.setId(stopPlace.getId());
        stop.setId(agencyAndId);

        // location type
        stop.setLocationType(Stop.LOCATION_TYPE_STATION);

        // name, description and platform code
        if (stopPlace.getName() != null) {
            stop.setName(stopPlace.getName().getValue());
        }
        // the description is set only if it is different from the name
        if (stopPlace.getDescription() != null
                && stopPlace.getDescription().getValue() != null
                && !stopPlace.getDescription().getValue().equals(stop.getName())) {
            stop.setDesc(stopPlace.getDescription().getValue());
        }
        if (stopPlace.getPrivateCode() != null) {
            stop.setPlatformCode(stopPlace.getPrivateCode().getValue());
        }

        // latitude and longitude
        stop.setLon(stopPlace.getCentroid().getLocation().getLongitude().doubleValue());
        stop.setLat(stopPlace.getCentroid().getLocation().getLatitude().doubleValue());

        // transport mode
        if (stopPlace.getTransportMode() != null) {
            stop.setVehicleType(getGtfsVehicleTypeFromNeTexTransportMode(stopPlace.getTransportMode()));
        } else {
            LOGGER.warn("Missing transport mode for stop place {}", stop.getId());
        }
        // accessibility
        if (stopPlace.getAccessibilityAssessment() != null) {
            String wheelchairAccess = stopPlace.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().value();
            if ("true".equals(wheelchairAccess)) {
                stop.setWheelchairBoarding(WHEELCHAIR_BOARDING_TRUE);

            } else if ("false".equals(wheelchairAccess)) {
                stop.setWheelchairBoarding(WHEELCHAIR_BOARDING_FALSE);
            }
        }

        return stop;
    }


    /**
     * Produce a GTFS stop from a NeTEx quay.
     * The GTFS name is copied from the parent StopPlace name since NeTEx Quays do not have a name (Nordic NeTEx profile requirement).
     * The GTFS vehicle type (GTFS extension) is inherited from the parent StopPlace since NeTEx Quays inherit transport mode from their parent StopPlace (Nordic NeTEx profile requirement).
     * The GTFS parent station is set as the NeTEx parent stop place
     * The GTFS location type is set to STOP
     * The GTFS description is based on the NeTEx description only if it is different from the NeTEx name, it is ignored otherwise.*
     *
     * @param quay the NeTEX Quay
     * @return the GTFS stop
     */
    public Stop produceStopFromQuay(Quay quay) {
        Stop stop = new Stop();
        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setAgencyId(defaultAgency.getId());
        agencyAndId.setId(quay.getId());
        stop.setId(agencyAndId);

        // location type
        stop.setLocationType(Stop.LOCATION_TYPE_STOP);

        // parent station
        StopPlace parentStopPlace = stopAreaRepository.getStopPlaceByQuayId(quay.getId());
        stop.setParentStation(parentStopPlace.getId());

        // name, description and platform code
        // the name of the quay is inherited from the parent stop place
        if (parentStopPlace.getName() != null) {
            stop.setName(parentStopPlace.getName().getValue());
        }
        // the description is set only if it is different from the name
        if (quay.getDescription() != null
                && quay.getDescription().getValue() != null
                && !quay.getDescription().getValue().equals(stop.getName())) {
            stop.setDesc(quay.getDescription().getValue());
        }
        if (quay.getPrivateCode() != null) {
            stop.setPlatformCode(quay.getPrivateCode().getValue());
        }

        // latitude and longitude
        stop.setLon(quay.getCentroid().getLocation().getLongitude().doubleValue());
        stop.setLat(quay.getCentroid().getLocation().getLatitude().doubleValue());

        // transport mode is inherited from the parent stop place
        if (parentStopPlace.getTransportMode() != null) {
            stop.setVehicleType(getGtfsVehicleTypeFromNeTexTransportMode(parentStopPlace.getTransportMode()));
        } else {
            LOGGER.warn("Missing transport mode for quay {}", stop.getId());
        }

        // accessibility
        if (quay.getAccessibilityAssessment() != null) {
            String wheelchairAccess = quay.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().value();
            if ("true".equals(wheelchairAccess)) {
                stop.setWheelchairBoarding(WHEELCHAIR_BOARDING_TRUE);

            } else if ("false".equals(wheelchairAccess)) {
                stop.setWheelchairBoarding(WHEELCHAIR_BOARDING_FALSE);
            }
        }

        return stop;
    }

    private int getGtfsVehicleTypeFromNeTexTransportMode(VehicleModeEnumeration netexTransportMode) {
        TransportModeNameEnum transportMode = NetexParserUtils.toTransportModeNameEnum(netexTransportMode.value());
        return RouteTypeEnum.from(transportMode, null).getValue();
    }

}
