package no.entur.damu.export.stop;

import java.io.InputStream;

/**
 * A stop area repository factory that builds stop area repositories from a NeTEx dataset archive.
 * The dataset can be refreshed at runtime by calling {@link #refreshStopAreaRepository(InputStream)}
 */
public class DefaultStopAreaRepositoryFactory implements StopAreaRepositoryFactory {

    private StopAreaRepository stopAreaRepository;

    @Override
    public synchronized StopAreaRepository getStopAreaRepository() {
        if (stopAreaRepository == null) {
            throw new IllegalStateException("The stop area repository is not loaded");
        }
        return stopAreaRepository;
    }

    /**
     * Refresh the cached stop area.
     *
     * @param stopDataset an input stream on a NeTEX dataset archive.
     */
    public synchronized void refreshStopAreaRepository(InputStream stopDataset) {
        DefaultStopAreaRepository defaultStopAreaRepository = new DefaultStopAreaRepository();
        defaultStopAreaRepository.loadStopAreas(stopDataset);
        this.stopAreaRepository = defaultStopAreaRepository;
    }
}
