package no.entur.damu.export.model;

/**
 * NeTEx transport sub-modes.
 */
public enum NetexTransportSubMode {

    // Air
    DomesticFlight,
    HelicopterService,
    InternationalFlight,

    // Bus
    AirportLinkBus,
    ExpressBus,
    LocalBus,
    NightBus,
    RailReplacementBus,
    RegionalBus,
    SchoolBus,
    ShuttleBus,
    SightseeingBus,

    // Coach
    InternationalCoach,
    NationalCoach,
    TouristCoach,

    // Metro
    Metro,

    // Rail
    International,
    InterregionalRail,
    Local,
    LongDistance,
    NightRail,
    RegionalRail,
    TouristRailway,
    AirportLinkRail,

    // Tram
    LocalTram,
    CityTram,

    // Water
    HighSpeedPassengerService,
    HighSpeedVehicleService,
    InternationalCarFerry,
    InternationalPassengerFerry,
    LocalCarFerry,
    LocalPassengerFerry,
    NationalCarFerry,
    SightseeingService,

    // Cableway
    Telecabin,

    // Funicular
    Funicular,

    // Unknown
    Unknown

}
