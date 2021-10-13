package no.entur.damu.netex;

import no.entur.damu.export.DefaultGtfsExporter;
import no.entur.damu.export.stop.StopAreaRepository;

import java.io.InputStream;

public class DamuGtfsExporter  extends DefaultGtfsExporter {
    public DamuGtfsExporter(String codespace, InputStream timetableDataset, StopAreaRepository stopAreaRepository) {
        super(codespace, timetableDataset, stopAreaRepository);
        setAgencyProducer(new DamuAgencyProducer(getNetexDatasetRepository()));
    }
}
