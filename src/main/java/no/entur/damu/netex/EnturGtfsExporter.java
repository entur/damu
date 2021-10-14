package no.entur.damu.netex;

import no.entur.damu.export.DefaultGtfsExporter;
import no.entur.damu.export.stop.StopAreaRepository;

/**
 * Custom GTFS exporter that handles missing or incomplete data in the input NeTEx dataset.
 */
public class EnturGtfsExporter extends DefaultGtfsExporter {
    public EnturGtfsExporter(String codespace, StopAreaRepository stopAreaRepository) {
        super(codespace, stopAreaRepository);

        setNetexDatasetLoader(new EnturNetexDatasetLoader());
        setAgencyProducer(new EnturAgencyProducer(getNetexDatasetRepository(), codespace));
        setFeedInfoProducer(new EnturFeedInfoProducer());
    }
}
