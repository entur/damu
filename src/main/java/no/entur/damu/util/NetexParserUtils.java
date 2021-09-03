package no.entur.damu.util;

import no.entur.damu.model.TransportModeNameEnum;
import no.entur.damu.model.TransportSubModeNameEnum;
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

    private static Logger log = LoggerFactory.getLogger(NetexParserUtils.class);

    public static TransportModeNameEnum toTransportModeNameEnum(String value) {
        if (value == null)
            return null;
        else if (value.equals("air"))
            return TransportModeNameEnum.Air;
        else if (value.equals("rail"))
            return TransportModeNameEnum.Rail;
        else if (value.equals("metro"))
            return TransportModeNameEnum.Metro;
        else if (value.equals("tram"))
            return TransportModeNameEnum.Tram;
        else if (value.equals("coach"))
            return TransportModeNameEnum.Coach;
        else if (value.equals("bus"))
            return TransportModeNameEnum.Bus;
        else if (value.equals("water"))
            return TransportModeNameEnum.Water;
        else if (value.equals("ferry"))
            return TransportModeNameEnum.Ferry;
        else if (value.equals("trolleyBus"))
            return TransportModeNameEnum.TrolleyBus;
        else if (value.equals("taxi"))
            return TransportModeNameEnum.Taxi;
        else if (value.equals("cableway"))
            return TransportModeNameEnum.Cableway;
        else if (value.equals("funicular"))
            return TransportModeNameEnum.Funicular;
        else if (value.equals("lift"))
            return TransportModeNameEnum.Lift;
        else if (value.equals("unknown"))
            return TransportModeNameEnum.Other;
        else if (value.equals("bicycle"))
            return TransportModeNameEnum.Bicycle;
        else
            return TransportModeNameEnum.Other;
    }

    public static TransportSubModeNameEnum toTransportSubModeNameEnum(TransportSubmodeStructure subModeStructure) {
        if (subModeStructure != null) {
            if (subModeStructure.getAirSubmode() != null) {
                AirSubmodeEnumeration mode = subModeStructure.getAirSubmode();
                switch (mode) {
                    case DOMESTIC_FLIGHT:
                        return TransportSubModeNameEnum.DomesticFlight;
                    case HELICOPTER_SERVICE:
                        return TransportSubModeNameEnum.HelicopterService;
                    case INTERNATIONAL_FLIGHT:
                        return TransportSubModeNameEnum.InternationalFlight;
                    default:
                        log.error("Unsupported air sub mode " + mode);
                }
            } else if (subModeStructure.getBusSubmode() != null) {
                BusSubmodeEnumeration mode = subModeStructure.getBusSubmode();
                switch (mode) {
                    case AIRPORT_LINK_BUS:
                        return TransportSubModeNameEnum.AirportLinkBus;
                    case EXPRESS_BUS:
                        return TransportSubModeNameEnum.ExpressBus;
                    case LOCAL_BUS:
                        return TransportSubModeNameEnum.LocalBus;
                    case NIGHT_BUS:
                        return TransportSubModeNameEnum.NightBus;
                    case RAIL_REPLACEMENT_BUS:
                        return TransportSubModeNameEnum.RailReplacementBus;
                    case REGIONAL_BUS:
                        return TransportSubModeNameEnum.RegionalBus;
                    case SCHOOL_BUS:
                        return TransportSubModeNameEnum.SchoolBus;
                    case SHUTTLE_BUS:
                        return TransportSubModeNameEnum.ShuttleBus;
                    case SIGHTSEEING_BUS:
                        return TransportSubModeNameEnum.SightseeingBus;
                    default:
                        log.error("Unsupported bus sub mode " + mode);
                }
            } else if (subModeStructure.getCoachSubmode() != null) {
                CoachSubmodeEnumeration mode = subModeStructure.getCoachSubmode();
                switch (mode) {
                    case TOURIST_COACH:
                        return TransportSubModeNameEnum.TouristCoach;
                    case INTERNATIONAL_COACH:
                        return TransportSubModeNameEnum.InternationalCoach;
                    case NATIONAL_COACH:
                        return TransportSubModeNameEnum.NationalCoach;
                    default:
                        log.error("Unsupported coach sub mode " + mode);
                }
            } else if (subModeStructure.getFunicularSubmode() != null) {
                FunicularSubmodeEnumeration mode = subModeStructure.getFunicularSubmode();
                switch (mode) {
                    case FUNICULAR:
                        return TransportSubModeNameEnum.Funicular;
                    default:
                        log.error("Unsupported funicular sub mode " + mode);
                }
            } else if (subModeStructure.getMetroSubmode() != null) {
                MetroSubmodeEnumeration mode = subModeStructure.getMetroSubmode();
                switch (mode) {
                    case METRO:
                        return TransportSubModeNameEnum.Metro;
                    default:
                        log.error("Unsupported metro sub mode " + mode);
                }
            } else if (subModeStructure.getRailSubmode() != null) {
                RailSubmodeEnumeration mode = subModeStructure.getRailSubmode();
                switch (mode) {
                    case INTERNATIONAL:
                        return TransportSubModeNameEnum.International;
                    case INTERREGIONAL_RAIL:
                        return TransportSubModeNameEnum.InterregionalRail;
                    case LOCAL:
                        return TransportSubModeNameEnum.Local;
                    case LONG_DISTANCE:
                        return TransportSubModeNameEnum.LongDistance;
                    case NIGHT_RAIL:
                        return TransportSubModeNameEnum.NightRail;
                    case REGIONAL_RAIL:
                        return TransportSubModeNameEnum.RegionalRail;
                    case TOURIST_RAILWAY:
                        return TransportSubModeNameEnum.TouristRailway;
                    case AIRPORT_LINK_RAIL:
                        return TransportSubModeNameEnum.AirportLinkRail;
                    default:
                        log.error("Unsupported rail sub mode " + mode);
                }
            } else if (subModeStructure.getTelecabinSubmode() != null) {
                TelecabinSubmodeEnumeration mode = subModeStructure.getTelecabinSubmode();
                switch (mode) {
                    case TELECABIN:
                        return TransportSubModeNameEnum.Telecabin;
                    default:
                        log.error("Unsupported telecabin sub mode " + mode);
                }
            } else if (subModeStructure.getTramSubmode() != null) {
                TramSubmodeEnumeration mode = subModeStructure.getTramSubmode();
                switch (mode) {
                    case LOCAL_TRAM:
                        return TransportSubModeNameEnum.LocalTram;
                    case CITY_TRAM:
                        return TransportSubModeNameEnum.CityTram;
                    default:
                        log.error("Unsupported tram sub mode " + mode);
                }
            } else if (subModeStructure.getWaterSubmode() != null) {
                WaterSubmodeEnumeration mode = subModeStructure.getWaterSubmode();
                switch (mode) {
                    case HIGH_SPEED_PASSENGER_SERVICE:
                        return TransportSubModeNameEnum.HighSpeedPassengerService;
                    case HIGH_SPEED_VEHICLE_SERVICE:
                        return TransportSubModeNameEnum.HighSpeedVehicleService;
                    case INTERNATIONAL_CAR_FERRY:
                        return TransportSubModeNameEnum.InternationalCarFerry;
                    case INTERNATIONAL_PASSENGER_FERRY:
                        return TransportSubModeNameEnum.InternationalPassengerFerry;
                    case LOCAL_CAR_FERRY:
                        return TransportSubModeNameEnum.LocalCarFerry;
                    case LOCAL_PASSENGER_FERRY:
                        return TransportSubModeNameEnum.LocalPassengerFerry;
                    case NATIONAL_CAR_FERRY:
                        return TransportSubModeNameEnum.NationalCarFerry;
                    case SIGHTSEEING_SERVICE:
                        return TransportSubModeNameEnum.SightseeingService;
                    default:
                        log.error("Unsupported water sub mode " + mode);
                }
            }

        }

        return null;
    }

}
