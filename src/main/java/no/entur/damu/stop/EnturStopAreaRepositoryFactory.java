package no.entur.damu.stop;

import org.entur.netex.gtfs.export.stop.DefaultStopAreaRepository;
import org.entur.netex.gtfs.export.stop.DefaultStopAreaRepositoryFactory;

import java.io.InputStream;

/**
 * A stop area repository factory that builds stop area repositories from a NeTEx dataset archive.
 * The dataset can be refreshed at runtime by calling {@link #refreshStopAreaRepository(InputStream)}
 */
public class EnturStopAreaRepositoryFactory extends DefaultStopAreaRepositoryFactory {

    private final QuayFetcher quayFetcher;
    private final StopPlaceFetcher stopPlaceFetcher;

    public EnturStopAreaRepositoryFactory(QuayFetcher quayFetcher, StopPlaceFetcher stopPlaceFetcher) {
        this.quayFetcher = quayFetcher;
        this.stopPlaceFetcher = stopPlaceFetcher;
    }

    /**
     * Refresh the cached stop area.
     *
     * @param stopDataset an input stream on a NeTEX dataset archive.
     */
    @Override
    public synchronized void refreshStopAreaRepository(InputStream stopDataset) {
        DefaultStopAreaRepository defaultStopAreaRepository =
                new DefaultStopAreaRepository(quayFetcher, stopPlaceFetcher);
        defaultStopAreaRepository.loadStopAreas(stopDataset);
        setStopAreaRepository(defaultStopAreaRepository);
    }
}
