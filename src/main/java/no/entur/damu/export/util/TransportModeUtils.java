/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *  *
 *
 */

package no.entur.damu.export.util;

import no.entur.damu.export.model.GtfsRouteType;
import org.rutebanken.netex.model.AirSubmodeEnumeration;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.CoachSubmodeEnumeration;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.TramSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.VehicleModeEnumeration;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static no.entur.damu.export.model.GtfsRouteType.AirService;
import static no.entur.damu.export.model.GtfsRouteType.BusService;
import static no.entur.damu.export.model.GtfsRouteType.CarHighSpeedFerryService;
import static no.entur.damu.export.model.GtfsRouteType.CityTramService;
import static no.entur.damu.export.model.GtfsRouteType.CoachService;
import static no.entur.damu.export.model.GtfsRouteType.DomesticAirService;
import static no.entur.damu.export.model.GtfsRouteType.ExpressBusService;
import static no.entur.damu.export.model.GtfsRouteType.FerryService;
import static no.entur.damu.export.model.GtfsRouteType.FunicularService;
import static no.entur.damu.export.model.GtfsRouteType.HelicopterAirService;
import static no.entur.damu.export.model.GtfsRouteType.HighSpeedRailService;
import static no.entur.damu.export.model.GtfsRouteType.InterRegionalRailService;
import static no.entur.damu.export.model.GtfsRouteType.InternationalAirService;
import static no.entur.damu.export.model.GtfsRouteType.InternationalCarFerryService;
import static no.entur.damu.export.model.GtfsRouteType.InternationalCoachService;
import static no.entur.damu.export.model.GtfsRouteType.InternationalPassengerFerryService;
import static no.entur.damu.export.model.GtfsRouteType.LocalBusService;
import static no.entur.damu.export.model.GtfsRouteType.LocalCarFerryService;
import static no.entur.damu.export.model.GtfsRouteType.LocalPassengerFerryService;
import static no.entur.damu.export.model.GtfsRouteType.LocalTramService;
import static no.entur.damu.export.model.GtfsRouteType.LongDistanceTrains;
import static no.entur.damu.export.model.GtfsRouteType.MetroService;
import static no.entur.damu.export.model.GtfsRouteType.MiscellaneousService;
import static no.entur.damu.export.model.GtfsRouteType.NationalCarFerryService;
import static no.entur.damu.export.model.GtfsRouteType.NationalCoachService;
import static no.entur.damu.export.model.GtfsRouteType.NightBusService;
import static no.entur.damu.export.model.GtfsRouteType.PassengerHighSpeedFerryService;
import static no.entur.damu.export.model.GtfsRouteType.RailReplacementBusService;
import static no.entur.damu.export.model.GtfsRouteType.RailwayService;
import static no.entur.damu.export.model.GtfsRouteType.RegionalBusService;
import static no.entur.damu.export.model.GtfsRouteType.RegionalRailService;
import static no.entur.damu.export.model.GtfsRouteType.SchoolBus;
import static no.entur.damu.export.model.GtfsRouteType.ShuttleBus;
import static no.entur.damu.export.model.GtfsRouteType.SightseeingBoatService;
import static no.entur.damu.export.model.GtfsRouteType.SightseeingBus;
import static no.entur.damu.export.model.GtfsRouteType.SleeperRailService;
import static no.entur.damu.export.model.GtfsRouteType.TaxiService;
import static no.entur.damu.export.model.GtfsRouteType.TelecabinService;
import static no.entur.damu.export.model.GtfsRouteType.TouristCoachService;
import static no.entur.damu.export.model.GtfsRouteType.TouristRailwayService;
import static no.entur.damu.export.model.GtfsRouteType.TramService;
import static no.entur.damu.export.model.GtfsRouteType.TrolleybusService;
import static no.entur.damu.export.model.GtfsRouteType.WaterTransportService;

public final class TransportModeUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportModeUtils.class);

    private static class TransportModeAndSubMode {
        private final String transportMode;
        private final String transportSubMode;

        TransportModeAndSubMode(String transportMode, String transportSubMode) {
            this.transportMode = transportMode;
            this.transportSubMode = transportSubMode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TransportModeAndSubMode that = (TransportModeAndSubMode) o;
            return transportMode.equals(that.transportMode) && Objects.equals(transportSubMode, that.transportSubMode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(transportMode, transportSubMode);
        }
    }

    private static final Map<TransportModeAndSubMode, GtfsRouteType> ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE;


    static {
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE = new HashMap<>();
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.AIR.value(), null), AirService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.AIR.value(), AirSubmodeEnumeration.DOMESTIC_FLIGHT.value()), DomesticAirService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.AIR.value(), AirSubmodeEnumeration.HELICOPTER_SERVICE.value()), HelicopterAirService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.AIR.value(), AirSubmodeEnumeration.INTERNATIONAL_FLIGHT.value()), InternationalAirService);

        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.BUS.value(), null), BusService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.BUS.value(), BusSubmodeEnumeration.AIRPORT_LINK_BUS.value()), BusService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.BUS.value(), BusSubmodeEnumeration.EXPRESS_BUS.value()), ExpressBusService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.BUS.value(), BusSubmodeEnumeration.LOCAL_BUS.value()), LocalBusService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.BUS.value(), BusSubmodeEnumeration.NIGHT_BUS.value()), NightBusService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.BUS.value(), BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.value()), RailReplacementBusService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.BUS.value(), BusSubmodeEnumeration.REGIONAL_BUS.value()), RegionalBusService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.BUS.value(), BusSubmodeEnumeration.SCHOOL_BUS.value()), SchoolBus);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.BUS.value(), BusSubmodeEnumeration.SHUTTLE_BUS.value()), ShuttleBus);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.BUS.value(), BusSubmodeEnumeration.SIGHTSEEING_BUS.value()), SightseeingBus);

        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.COACH.value(), null), CoachService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.COACH.value(), CoachSubmodeEnumeration.INTERNATIONAL_COACH.value()), InternationalCoachService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.COACH.value(), CoachSubmodeEnumeration.NATIONAL_COACH.value()), NationalCoachService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.COACH.value(), CoachSubmodeEnumeration.TOURIST_COACH.value()), TouristCoachService);

        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(VehicleModeEnumeration.FERRY.value(), null), FerryService);

        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.METRO.value(), null), MetroService);

        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.RAIL.value(), null), RailwayService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.RAIL.value(), RailSubmodeEnumeration.INTERNATIONAL.value()), LongDistanceTrains);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.RAIL.value(), RailSubmodeEnumeration.LONG_DISTANCE.value()), LongDistanceTrains);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.RAIL.value(), RailSubmodeEnumeration.INTERREGIONAL_RAIL.value()), InterRegionalRailService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.RAIL.value(), RailSubmodeEnumeration.LOCAL.value()), RailwayService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.RAIL.value(), RailSubmodeEnumeration.NIGHT_RAIL.value()), SleeperRailService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.RAIL.value(), RailSubmodeEnumeration.REGIONAL_RAIL.value()), RegionalRailService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.RAIL.value(), RailSubmodeEnumeration.TOURIST_RAILWAY.value()), TouristRailwayService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.RAIL.value(), RailSubmodeEnumeration.AIRPORT_LINK_RAIL.value()), HighSpeedRailService);

        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.TROLLEY_BUS.value(), null), TrolleybusService);

        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.TRAM.value(), null), TramService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.TRAM.value(), TramSubmodeEnumeration.LOCAL_TRAM.value()), LocalTramService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.TRAM.value(), TramSubmodeEnumeration.CITY_TRAM.value()), CityTramService);

        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.WATER.value(), null), WaterTransportService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.WATER.value(), WaterSubmodeEnumeration.HIGH_SPEED_PASSENGER_SERVICE.value()), PassengerHighSpeedFerryService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.WATER.value(), WaterSubmodeEnumeration.HIGH_SPEED_VEHICLE_SERVICE.value()), CarHighSpeedFerryService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.WATER.value(), WaterSubmodeEnumeration.INTERNATIONAL_CAR_FERRY.value()), InternationalCarFerryService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.WATER.value(), WaterSubmodeEnumeration.INTERNATIONAL_PASSENGER_FERRY.value()), InternationalPassengerFerryService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.WATER.value(), WaterSubmodeEnumeration.LOCAL_CAR_FERRY.value()), LocalCarFerryService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.WATER.value(), WaterSubmodeEnumeration.LOCAL_PASSENGER_FERRY.value()), LocalPassengerFerryService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.WATER.value(), WaterSubmodeEnumeration.NATIONAL_CAR_FERRY.value()), NationalCarFerryService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.WATER.value(), WaterSubmodeEnumeration.SIGHTSEEING_SERVICE.value()), SightseeingBoatService);

        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.CABLEWAY.value(), null), TelecabinService);
        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(VehicleModeEnumeration.LIFT.value(), null), TelecabinService);

        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.FUNICULAR.value(), null), FunicularService);

        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(AllVehicleModesOfTransportEnumeration.TAXI.value(), null), TaxiService);

        ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.put(new TransportModeAndSubMode(VehicleModeEnumeration.OTHER.value(), null), MiscellaneousService);
    }


    private TransportModeUtils() {
    }

    /**
     * Return the GTFS extended route type code for a given NeTEx Line:
     *
     * @param line a NeTEx line.
     * @return the GTFS extended route type code.
     */
    public static int getGtfsExtendedRouteType(Line line) {
        String transportMode = line.getTransportMode().value();
        String transportSubMode = getSubMode(line.getTransportSubmode());
        return getGtfsExtendedRouteType(transportMode, transportSubMode).getValue();
    }

    /**
     * Return the GTFS extended route type code for a NeTEx netexTransportMode:
     *
     * @param transportMode a NeTEx transport mode.
     * @return the GTFS extended route type code.
     */
    public static int getGtfsExtendedRouteType(VehicleModeEnumeration transportMode) {
        return getGtfsExtendedRouteType(transportMode.value(), null).getValue();
    }

    /**
     * Convert a pair of NeTEx (transport mode, transport submode) into a GTFS extended route type.
     *
     * @param transportMode    a NeTEx transport mode.
     * @param transportSubMode a NeTEx transport submode.
     * @return a GTFS extended route type.
     */
    private static GtfsRouteType getGtfsExtendedRouteType(String transportMode, String transportSubMode) {
        GtfsRouteType routeType = ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.get(new TransportModeAndSubMode(transportMode, transportSubMode));
        if (routeType == null) {
            LOGGER.debug("Unknown transport sub mode {}, falling back to route type for parent transport mode {}", transportSubMode, transportMode);
            routeType = ROUTE_TYPE_FOR_TRANSPORT_MODE_AND_SUB_MODE.get(new TransportModeAndSubMode(transportMode, null));
        }
        if (routeType == null) {
            LOGGER.debug("Unknown transport mode {}, falling back to route type for miscellaneous services", transportMode);
            routeType = MiscellaneousService;
        }
        return routeType;

    }


    private static String getSubMode(TransportSubmodeStructure subModeStructure) {
        if (subModeStructure.getAirSubmode() != null) {
            return subModeStructure.getAirSubmode().value();
        }
        if (subModeStructure.getBusSubmode() != null) {
            return subModeStructure.getBusSubmode().value();
        }
        if (subModeStructure.getCoachSubmode() != null) {
            return subModeStructure.getCoachSubmode().value();
        }
        if (subModeStructure.getFunicularSubmode() != null) {
            return subModeStructure.getFunicularSubmode().value();
        }
        if (subModeStructure.getMetroSubmode() != null) {
            return subModeStructure.getMetroSubmode().value();
        }
        if (subModeStructure.getRailSubmode() != null) {
            return subModeStructure.getRailSubmode().value();
        }
        if (subModeStructure.getTelecabinSubmode() != null) {
            return subModeStructure.getTelecabinSubmode().value();
        }
        if (subModeStructure.getTramSubmode() != null) {
            return subModeStructure.getTramSubmode().value();
        }
        if (subModeStructure.getSnowAndIceSubmode() != null) {
            return subModeStructure.getSnowAndIceSubmode().value();
        }
        if (subModeStructure.getWaterSubmode() != null) {
            return subModeStructure.getWaterSubmode().value();
        }
        return null;
    }
}
