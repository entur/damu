package no.entur.damu.export.model;


/**
 * NeTEx Transport modes.
 */
public enum NetexTransportMode {
    Air("air"),
    Bus("bus"),
    Coach("coach"),
    Ferry("ferry"),
    Metro("metro"),
    Rail("rail"),
    TrolleyBus("trolleyBus"),
    Tram("tram"),
    Water("water"),
    Cableway("cableway"),
    Funicular("funicular"),
    Lift("lift"),
    Taxi("taxi"),
    Bicycle("bicycle"),
    Other("other");

    private final String transportMode;

    NetexTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }
    public String getTransportMode() {
        return transportMode;
    }
}
