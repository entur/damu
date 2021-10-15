package no.entur.damu.export.util;

import no.entur.damu.export.model.NetexTransportMode;
import no.entur.damu.export.model.NetexTransportSubMode;
import org.rutebanken.netex.model.AirSubmodeEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.CoachSubmodeEnumeration;
import org.rutebanken.netex.model.FunicularSubmodeEnumeration;
import org.rutebanken.netex.model.MetroSubmodeEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.TelecabinSubmodeEnumeration;
import org.rutebanken.netex.model.TramSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetexParserUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetexParserUtils.class);

    public static NetexTransportMode toTransportModeNameEnum(String value) {
        if (value == null)
            return null;
        else if (value.equals("air"))
            return NetexTransportMode.Air;
        else if (value.equals("rail"))
            return NetexTransportMode.Rail;
        else if (value.equals("metro"))
            return NetexTransportMode.Metro;
        else if (value.equals("tram"))
            return NetexTransportMode.Tram;
        else if (value.equals("coach"))
            return NetexTransportMode.Coach;
        else if (value.equals("bus"))
            return NetexTransportMode.Bus;
        else if (value.equals("water"))
            return NetexTransportMode.Water;
        else if (value.equals("ferry"))
            return NetexTransportMode.Ferry;
        else if (value.equals("trolleyBus"))
            return NetexTransportMode.TrolleyBus;
        else if (value.equals("taxi"))
            return NetexTransportMode.Taxi;
        else if (value.equals("cableway"))
            return NetexTransportMode.Cableway;
        else if (value.equals("funicular"))
            return NetexTransportMode.Funicular;
        else if (value.equals("lift"))
            return NetexTransportMode.Lift;
        else if (value.equals("unknown"))
            return NetexTransportMode.Other;
        else if (value.equals("bicycle"))
            return NetexTransportMode.Bicycle;
        else
            return NetexTransportMode.Other;
    }

    public static NetexTransportSubMode toTransportSubModeNameEnum(TransportSubmodeStructure subModeStructure) {
        if (subModeStructure != null) {
            if (subModeStructure.getAirSubmode() != null) {
                AirSubmodeEnumeration mode = subModeStructure.getAirSubmode();
                switch (mode) {
                    case DOMESTIC_FLIGHT:
                        return NetexTransportSubMode.DomesticFlight;
                    case HELICOPTER_SERVICE:
                        return NetexTransportSubMode.HelicopterService;
                    case INTERNATIONAL_FLIGHT:
                        return NetexTransportSubMode.InternationalFlight;
                    default:
                        LOGGER.error("Unsupported air sub mode " + mode);
                }
            } else if (subModeStructure.getBusSubmode() != null) {
                BusSubmodeEnumeration mode = subModeStructure.getBusSubmode();
                switch (mode) {
                    case AIRPORT_LINK_BUS:
                        return NetexTransportSubMode.AirportLinkBus;
                    case EXPRESS_BUS:
                        return NetexTransportSubMode.ExpressBus;
                    case LOCAL_BUS:
                        return NetexTransportSubMode.LocalBus;
                    case NIGHT_BUS:
                        return NetexTransportSubMode.NightBus;
                    case RAIL_REPLACEMENT_BUS:
                        return NetexTransportSubMode.RailReplacementBus;
                    case REGIONAL_BUS:
                        return NetexTransportSubMode.RegionalBus;
                    case SCHOOL_BUS:
                        return NetexTransportSubMode.SchoolBus;
                    case SHUTTLE_BUS:
                        return NetexTransportSubMode.ShuttleBus;
                    case SIGHTSEEING_BUS:
                        return NetexTransportSubMode.SightseeingBus;
                    default:
                        LOGGER.error("Unsupported bus sub mode " + mode);
                }
            } else if (subModeStructure.getCoachSubmode() != null) {
                CoachSubmodeEnumeration mode = subModeStructure.getCoachSubmode();
                switch (mode) {
                    case TOURIST_COACH:
                        return NetexTransportSubMode.TouristCoach;
                    case INTERNATIONAL_COACH:
                        return NetexTransportSubMode.InternationalCoach;
                    case NATIONAL_COACH:
                        return NetexTransportSubMode.NationalCoach;
                    default:
                        LOGGER.error("Unsupported coach sub mode " + mode);
                }
            } else if (subModeStructure.getFunicularSubmode() != null) {
                FunicularSubmodeEnumeration mode = subModeStructure.getFunicularSubmode();
                switch (mode) {
                    case FUNICULAR:
                        return NetexTransportSubMode.Funicular;
                    default:
                        LOGGER.error("Unsupported funicular sub mode " + mode);
                }
            } else if (subModeStructure.getMetroSubmode() != null) {
                MetroSubmodeEnumeration mode = subModeStructure.getMetroSubmode();
                switch (mode) {
                    case METRO:
                        return NetexTransportSubMode.Metro;
                    default:
                        LOGGER.error("Unsupported metro sub mode " + mode);
                }
            } else if (subModeStructure.getRailSubmode() != null) {
                RailSubmodeEnumeration mode = subModeStructure.getRailSubmode();
                switch (mode) {
                    case INTERNATIONAL:
                        return NetexTransportSubMode.International;
                    case INTERREGIONAL_RAIL:
                        return NetexTransportSubMode.InterregionalRail;
                    case LOCAL:
                        return NetexTransportSubMode.Local;
                    case LONG_DISTANCE:
                        return NetexTransportSubMode.LongDistance;
                    case NIGHT_RAIL:
                        return NetexTransportSubMode.NightRail;
                    case REGIONAL_RAIL:
                        return NetexTransportSubMode.RegionalRail;
                    case TOURIST_RAILWAY:
                        return NetexTransportSubMode.TouristRailway;
                    case AIRPORT_LINK_RAIL:
                        return NetexTransportSubMode.AirportLinkRail;
                    default:
                        LOGGER.error("Unsupported rail sub mode " + mode);
                }
            } else if (subModeStructure.getTelecabinSubmode() != null) {
                TelecabinSubmodeEnumeration mode = subModeStructure.getTelecabinSubmode();
                switch (mode) {
                    case TELECABIN:
                        return NetexTransportSubMode.Telecabin;
                    default:
                        LOGGER.error("Unsupported telecabin sub mode " + mode);
                }
            } else if (subModeStructure.getTramSubmode() != null) {
                TramSubmodeEnumeration mode = subModeStructure.getTramSubmode();
                switch (mode) {
                    case LOCAL_TRAM:
                        return NetexTransportSubMode.LocalTram;
                    case CITY_TRAM:
                        return NetexTransportSubMode.CityTram;
                    default:
                        LOGGER.error("Unsupported tram sub mode " + mode);
                }
            } else if (subModeStructure.getWaterSubmode() != null) {
                WaterSubmodeEnumeration mode = subModeStructure.getWaterSubmode();
                switch (mode) {
                    case HIGH_SPEED_PASSENGER_SERVICE:
                        return NetexTransportSubMode.HighSpeedPassengerService;
                    case HIGH_SPEED_VEHICLE_SERVICE:
                        return NetexTransportSubMode.HighSpeedVehicleService;
                    case INTERNATIONAL_CAR_FERRY:
                        return NetexTransportSubMode.InternationalCarFerry;
                    case INTERNATIONAL_PASSENGER_FERRY:
                        return NetexTransportSubMode.InternationalPassengerFerry;
                    case LOCAL_CAR_FERRY:
                        return NetexTransportSubMode.LocalCarFerry;
                    case LOCAL_PASSENGER_FERRY:
                        return NetexTransportSubMode.LocalPassengerFerry;
                    case NATIONAL_CAR_FERRY:
                        return NetexTransportSubMode.NationalCarFerry;
                    case SIGHTSEEING_SERVICE:
                        return NetexTransportSubMode.SightseeingService;
                    default:
                        LOGGER.error("Unsupported water sub mode " + mode);
                }
            }

        }

        return null;
    }



}
