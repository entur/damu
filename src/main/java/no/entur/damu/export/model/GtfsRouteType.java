package no.entur.damu.export.model;


/**
 * https://developers.google.com/transit/gtfs/reference/extended-route-types
 */
public enum GtfsRouteType {


    // Basic types
    Tram(0),
    Subway(1),
    Rail(2),
    Bus(3),
    Ferry(4),
    Cable(5),
    Gondola(6),
    Funicular(7),

    // Extended types : Rail
    RailwayService(100),
    HighSpeedRailService(101),
    LongDistanceTrains(102),
    InterRegionalRailService(103),
    CarTransportRailService(104),
    SleeperRailService(105),
    RegionalRailService(106),
    TouristRailwayService(107),
    RailShuttleWithinComplex(108),
    SuburbanRailway(109),
    ReplacementRailService(110),
    SpecialRailService(111),
    LorryTransportRailService(112),
    AllRailServices(113),
    CrossCountryRailService(114),
    VehicleTransportRailService(115),
    RackandPinionRailway(116),
    AdditionalRailService(117),

    // Extended types : Coach
    CoachService(200),
    InternationalCoachService(201),
    NationalCoachService(202),
    ShuttleCoachService(203),
    RegionalCoachService(204),
    SpecialCoachService(205),
    SightseeingCoachService(206),
    TouristCoachService(207),
    CommuterCoachService(208),
    AllCoachServices(209),

    // Extended types : Suburban Rail
    SuburbanRailwayService(300),

    // Extended types : Urban Rail
    UrbanRailwayService(400),
    MetroService(401),
    UndergroundService(402),
    UrbanRailwayService2(403),
    AllUrbanRailwayServices(404),
    Monorail(405),

    // Extended types : Metro
    MetroService2(500),

    // Extended types : Underground
    UndergroundService2(600),

    // Extended types : Bus
    BusService(700),
    RegionalBusService(701),
    ExpressBusService(702),
    StoppingBusService(703),
    LocalBusService(704),
    NightBusService(705),
    PostBusService(706),
    SpecialNeedsBus(707),
    MobilityBusService(708),
    MobilityBusforRegisteredDisabled(709),
    SightseeingBus(710),
    ShuttleBus(711),
    SchoolBus(712),
    SchoolandPublicServiceBus(713),
    RailReplacementBusService(714),
    DemandandResponseBusService(715),
    AllBusServices(716),

    // Extended types : Trolleybus
    TrolleybusService(800),

    // Extended types : Tram
    TramService(900),
    CityTramService(901),
    LocalTramService(902),
    RegionalTramService(903),
    SightseeingTramService(904),
    ShuttleTramService(905),
    AllTramServices(906),

    // Extended types : Water
    WaterTransportService(1000),
    InternationalCarFerryService(1001),
    NationalCarFerryService(1002),
    RegionalCarFerryService(1003),
    LocalCarFerryService(1004),
    InternationalPassengerFerryService(1005),
    NationalPassengerFerryService(1006),
    RegionalPassengerFerryService(1007),
    LocalPassengerFerryService(1008),
    PostBoatService(1009),
    TrainFerryService(1010),
    RoadLinkFerryService(1011),
    AirportLinkFerryService(1012),
    CarHighSpeedFerryService(1013),
    PassengerHighSpeedFerryService(1014),
    SightseeingBoatService(1015),
    SchoolBoat(1016),
    CableDrawnBoatService(1017),
    RiverBusService(1018),
    ScheduledFerryService(1019),
    ShuttleFerryService(1020),
    AllWaterTransportServices(1021),


    // Extended types : Air
    AirService(1100),
    InternationalAirService(1101),
    DomesticAirService(1102),
    IntercontinentalAirService(1103),
    DomesticScheduledAirService(1104),
    ShuttleAirService(1105),
    IntercontinentalCharterAirService(1106),
    InternationalCharterAirService(1107),
    RoundTripCharterAirService(1108),
    SightseeingAirService(1109),
    HelicopterAirService(1110),
    DomesticCharterAirService(1111),
    SchengenAreaAirService(1112),
    AirshipService(1113),
    AllAirServices(1114),

    // Extended types : Ferry
    FerryService(1200),

    // Extended types : Telecabin
    TelecabinService(1300),
    TelecabinService2(1301),
    CableCarService(1302),
    ElevatorService(1303),
    ChairLiftService(1304),
    DragLiftService(1305),
    SmallTelecabinService(1306),
    AllTelecabinServices(1307),

    // Extended types : Funicular
    FunicularService(1400),
    FunicularService2(1401),
    AllFunicularService(1402),

    // Extended types : Taxi
    TaxiService(1500),
    CommunalTaxiService(1501),
    WaterTaxiService(1502),
    RailTaxiService(1503),
    BikeTaxiService(1504),
    LicensedTaxiService(1505),
    PrivateHireServiceVehicle(1506),
    AllTaxiServices(1507),

    // Extended types : Self-drive
    SelfDrive(1600),
    HireCar(1601),
    HireVan(1602),
    HireMotorbike(1603),
    HireCycle(1604),

    /// Extended types : Miscellaneous
    MiscellaneousService(1700),
    CableCar(1701),
    HorseDrawnCarriage(1702);

    private final int value;

    GtfsRouteType(final int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public int getValue() {
        return value;
    }

    public static GtfsRouteType from(NetexTransportMode transportMode, NetexTransportSubMode subMode) {
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
