package no.entur.damu.export.stop;

import no.entur.damu.export.exception.NetexParsingException;
import no.entur.damu.export.util.NetexDatasetParserUtil;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

/**
 * A Stop area repository that loads data from a NeTEx dataset archive.
 */
public class FileStopAreaRepository implements StopAreaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStopAreaRepository.class);

    private final NetexParser netexParser;
    private Map<String, StopPlace> stopPlaceByQuayId;
    private Map<String, Quay> quayById;


    public FileStopAreaRepository() {
        this.netexParser = new NetexParser();
    }

    public void loadStopAreas(InputStream stopDataset) {

        LOGGER.info("Importing NeTEx Stop dataset");

        NetexEntitiesIndex netexStopEntitiesIndex = new NetexEntitiesIndexImpl();

        try (ZipInputStream zipInputStream = new ZipInputStream(stopDataset)) {
            NetexDatasetParserUtil.parse(netexParser, zipInputStream, netexStopEntitiesIndex);
        } catch (IOException e) {
            throw new NetexParsingException("Error while parsing the NeTEx stop dataset", e);
        }

        stopPlaceByQuayId = netexStopEntitiesIndex.getStopPlaceIdByQuayIdIndex()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> netexStopEntitiesIndex.getStopPlaceIndex().getLatestVersion(entry.getValue())));

        quayById = netexStopEntitiesIndex.getQuayIndex().getAllVersions()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> netexStopEntitiesIndex.getQuayIndex().getLatestVersion(entry.getKey())));

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
