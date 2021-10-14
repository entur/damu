package no.entur.damu.export.stop;

import no.entur.damu.export.loader.DefaultNetexDatasetLoader;
import no.entur.damu.export.loader.NetexDatasetLoader;
import no.entur.damu.export.repository.DefaultNetexDatasetRepository;
import no.entur.damu.export.repository.NetexDatasetRepository;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A Stop area repository that loads data from a NeTEx dataset archive.
 */
public class DefaultStopAreaRepository implements StopAreaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStopAreaRepository.class);

    private Map<String, StopPlace> stopPlaceByQuayId;
    private Map<String, Quay> quayById;

    private final NetexDatasetLoader netexDatasetLoader;


    public DefaultStopAreaRepository() {
        this.netexDatasetLoader = new DefaultNetexDatasetLoader();
    }

    public void loadStopAreas(InputStream stopDataset) {

        LOGGER.info("Importing NeTEx Stop dataset");
        NetexDatasetRepository netexStopRepository = new DefaultNetexDatasetRepository();
        netexDatasetLoader.load(stopDataset, netexStopRepository);
        NetexEntitiesIndex netexStopEntitiesIndex = netexStopRepository.getIndex();

        stopPlaceByQuayId = netexStopEntitiesIndex.getStopPlaceIdByQuayIdIndex()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> netexStopEntitiesIndex.getStopPlaceIndex().getLatestVersion(entry.getValue())));

        quayById = netexStopEntitiesIndex.getQuayIndex().getAllVersions()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> netexStopEntitiesIndex.getQuayIndex().getLatestVersion(entry.getKey())));

        LOGGER.info("Imported NeTEx Stop dataset");

    }

    @Override
    public StopPlace getStopPlaceByQuayId(String quayId) {
        return stopPlaceByQuayId.get(quayId);
    }

    @Override
    public Quay getQuayById(String quayId) {
        return quayById.get(quayId);
    }
}
