package no.entur.damu.export.loader;

import no.entur.damu.export.repository.NetexDatasetRepository;

import java.io.InputStream;

/**
 * Load a NeTEx dataset into memory.
 */
public interface NetexDatasetLoader {

    /**
     * Load a NeTEX dataset archive into an in-memory repository
     * @param timetableDataset a ZIP file containing the NeTEx dataset
     * @param netexDatasetRepository an in-memory repository containing the NeTEx entities.
     */
    void load(InputStream timetableDataset, NetexDatasetRepository netexDatasetRepository);
}
