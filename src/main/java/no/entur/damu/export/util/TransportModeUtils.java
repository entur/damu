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
import no.entur.damu.export.model.NetexTransportMode;
import no.entur.damu.export.model.NetexTransportSubMode;
import org.rutebanken.netex.model.AirSubmodeEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.CoachSubmodeEnumeration;
import org.rutebanken.netex.model.FunicularSubmodeEnumeration;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.MetroSubmodeEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.TelecabinSubmodeEnumeration;
import org.rutebanken.netex.model.TramSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.VehicleModeEnumeration;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private static final Map<String, NetexTransportMode> TRANSPORT_MODES = Arrays.stream(NetexTransportMode.values()).collect(Collectors.toMap(NetexTransportMode::getTransportMode, Function.identity()));

    private TransportModeUtils() {
    }

    /**
     * Convert a literal transport mode into a NetexTransportMode enum.
     *
     * @param value a NeTEx transport mode string.
     * @return a NeTEx transport mode enum.
     */
    public static NetexTransportMode toNetexTransportMode(String value) {
        if (value == null) {
            return null;
        }
        NetexTransportMode netexTransportMode = TRANSPORT_MODES.get(value);
        if (netexTransportMode != null) {
            return netexTransportMode;
        } else {
            return NetexTransportMode.Other;
        }
    }

    /**
     * Convert a literal transport submode into a NetexTransportSubMode enum.
     *
     * @param subModeStructure a literal transport submode
     * @return a NetexTransportSubMode enum
     */
    public static NetexTransportSubMode toNetexTransportSubMode(TransportSubmodeStructure subModeStructure) {
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
                        LOGGER.warn("Unsupported air sub mode {}", mode);
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
                        LOGGER.warn("Unsupported bus sub mode {}", mode);
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
                        LOGGER.warn("Unsupported coach sub mode {} ", mode);
                }
            } else if (subModeStructure.getFunicularSubmode() != null) {
                FunicularSubmodeEnumeration mode = subModeStructure.getFunicularSubmode();
                switch (mode) {
                    case FUNICULAR:
                        return NetexTransportSubMode.Funicular;
                    default:
                        LOGGER.warn("Unsupported funicular sub mode {}", mode);
                }
            } else if (subModeStructure.getMetroSubmode() != null) {
                MetroSubmodeEnumeration mode = subModeStructure.getMetroSubmode();
                switch (mode) {
                    case METRO:
                        return NetexTransportSubMode.Metro;
                    default:
                        LOGGER.warn("Unsupported metro sub mode {}", mode);
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
                        LOGGER.warn("Unsupported rail sub mode {}", mode);
                }
            } else if (subModeStructure.getTelecabinSubmode() != null) {
                TelecabinSubmodeEnumeration mode = subModeStructure.getTelecabinSubmode();
                switch (mode) {
                    case TELECABIN:
                        return NetexTransportSubMode.Telecabin;
                    default:
                        LOGGER.warn("Unsupported telecabin sub mode {}", mode);
                }
            } else if (subModeStructure.getTramSubmode() != null) {
                TramSubmodeEnumeration mode = subModeStructure.getTramSubmode();
                switch (mode) {
                    case LOCAL_TRAM:
                        return NetexTransportSubMode.LocalTram;
                    case CITY_TRAM:
                        return NetexTransportSubMode.CityTram;
                    default:
                        LOGGER.warn("Unsupported tram sub mode {}", mode);
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
                        LOGGER.warn("Unsupported water sub mode {}", mode);
                }
            }
        }
        return null;
    }

    /**
     * Return the GTFS extended route type code for a given NeTEx Line:
     *
     * @param line a NeTEx line.
     * @return the GTFS extended route type code.
     */
    public static int getGtfsExtendedRouteType(Line line) {
        NetexTransportMode transportMode = TransportModeUtils.toNetexTransportMode(line.getTransportMode().value());
        NetexTransportSubMode transportSubMode = TransportModeUtils.toNetexTransportSubMode(line.getTransportSubmode());
        return getGtfsExtendedRouteType(transportMode, transportSubMode).getValue();
    }

    /**
     * Return the GTFS extended route type code for a NeTEx netexTransportMode:
     *
     * @param netexTransportMode a NeTEx transport mode.
     * @return the GTFS extended route type code.
     */
    public static int getGtfsExtendedRouteType(VehicleModeEnumeration netexTransportMode) {
        NetexTransportMode transportMode = TransportModeUtils.toNetexTransportMode(netexTransportMode.value());
        return getGtfsExtendedRouteType(transportMode, null).getValue();
    }

    /**
     * Convert a pair of NeTEx (transport mode, transport submode) into a GTFS extended route type.
     *
     * @param transportMode a NeTEx transport mode.
     * @param subMode       a NeTEx transport submode.
     * @return a GTFS extended route type.
     */
    private static GtfsRouteType getGtfsExtendedRouteType(NetexTransportMode transportMode, NetexTransportSubMode subMode) {
        switch (transportMode) {
            case Air: {
                if (subMode == null) {
                    return AirService;
                } else {
                    switch (subMode) {
                        case DomesticFlight:
                            return DomesticAirService;
                        case HelicopterService:
                            return HelicopterAirService;
                        case InternationalFlight:
                            return InternationalAirService;
                        default:
                            return AirService;
                    }
                }
            }
            case Bus: {
                if (subMode == null) {
                    return BusService;
                } else {
                    switch (subMode) {
                        case AirportLinkBus:
                            return BusService;
                        case ExpressBus:
                            return ExpressBusService;
                        case LocalBus:
                            return LocalBusService;
                        case NightBus:
                            return NightBusService;
                        case RailReplacementBus:
                            return RailReplacementBusService;
                        case RegionalBus:
                            return RegionalBusService;
                        case SchoolBus:
                            return SchoolBus;
                        case ShuttleBus:
                            return ShuttleBus;
                        case SightseeingBus:
                            return SightseeingBus;
                        default:
                            return BusService;
                    }
                }
            }
            case Coach:
                if (subMode == null) {
                    return CoachService;
                } else {
                    switch (subMode) {
                        case InternationalCoach:
                            return InternationalCoachService;
                        case NationalCoach:
                            return NationalCoachService;
                        case TouristCoach:
                            return TouristCoachService;
                        default:
                            return CoachService;
                    }
                }

            case Ferry:
                return FerryService;
            case Metro:
                return MetroService;
            case Rail:
                if (subMode == null) {
                    return RailwayService;
                } else {
                    switch (subMode) {
                        case International:
                        case LongDistance:
                            return LongDistanceTrains;
                        case InterregionalRail:
                            return InterRegionalRailService;
                        case Local:
                            return RailwayService;
                        case NightRail:
                            return SleeperRailService;
                        case RegionalRail:
                            return RegionalRailService;
                        case TouristRailway:
                            return TouristRailwayService;
                        case AirportLinkRail:
                            return HighSpeedRailService;
                        default:
                            return RailwayService;
                    }
                }

            case TrolleyBus:
                return TrolleybusService;
            case Tram:
                if (subMode == null) {
                    return TramService;
                } else {
                    switch (subMode) {
                        case LocalTram:
                            return LocalTramService;
                        case CityTram:
                            return CityTramService;
                        default:
                            return TramService;
                    }
                }
            case Water:
                if (subMode == null) {
                    return WaterTransportService;
                } else {
                    switch (subMode) {
                        case HighSpeedPassengerService:
                            return PassengerHighSpeedFerryService;
                        case HighSpeedVehicleService:
                            return CarHighSpeedFerryService;
                        case InternationalCarFerry:
                            return InternationalCarFerryService;
                        case InternationalPassengerFerry:
                            return InternationalPassengerFerryService;
                        case LocalCarFerry:
                            return LocalCarFerryService;
                        case LocalPassengerFerry:
                            return LocalPassengerFerryService;
                        case NationalCarFerry:
                            return NationalCarFerryService;
                        case SightseeingService:
                            return SightseeingBoatService;
                        default:
                            return WaterTransportService;
                    }
                }
            case Cableway:
            case Lift:
                return TelecabinService;
            case Funicular:
                return FunicularService;
            case Taxi:
                return TaxiService;
            case Bicycle:
            case Other:
            default:
                return MiscellaneousService;
        }
    }
}
