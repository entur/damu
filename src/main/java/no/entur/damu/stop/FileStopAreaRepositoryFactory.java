package no.entur.damu.stop;

import no.entur.damu.exception.DamuException;

import java.io.InputStream;

/**
 * A stop area repository that builds stop area repositories from a NeTEx dataset archive.
 */
public class FileStopAreaRepositoryFactory implements StopAreaRepositoryFactory {

    private StopAreaRepository stopAreaRepository;

    @Override
    public synchronized StopAreaRepository getStopAreaRepository() {
        if(stopAreaRepository == null) {
            throw new DamuException("The stop area repository is not loaded");
        }
        return stopAreaRepository;
    }

    /**
     * Refresh the cached stop area.
     * @param stopDataset an input stream on a NeTEX dataset archive.
     */
    public synchronized void refreshStopAreRepository(InputStream stopDataset) {
        FileStopAreaRepository fileStopAreaRepository = new FileStopAreaRepository();
        fileStopAreaRepository.loadStopAreas(stopDataset);
        this.stopAreaRepository = fileStopAreaRepository;
    }
}
