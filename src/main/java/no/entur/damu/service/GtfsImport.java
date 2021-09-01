package no.entur.damu.service;

import no.entur.damu.exception.GtfsImportException;
import no.entur.damu.producer.RouteProducer;
import no.entur.damu.producer.StopProducer;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.serialization.GtfsWriter;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.gtfs.services.GtfsDao;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import no.entur.damu.producer.AgencyProducer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GtfsImport {

    private static final Logger LOGGER = LoggerFactory.getLogger(GtfsImport.class);

    private final NetexParser netexParser;
    private final NetexEntitiesIndex netexTimetableEntitiesIndex;
    private final NetexEntitiesIndex netexStopEntitiesIndex;
    private final GenericMutableDao gtfsDao;

    private final InputStream timetableDataset;
    private final InputStream stopDataset;

    public GtfsImport(InputStream timetableDataset, InputStream stopDataset) {
        this.timetableDataset = timetableDataset;
        this.stopDataset = stopDataset;
        this.netexParser = new NetexParser();
        this.netexTimetableEntitiesIndex = new NetexEntitiesIndexImpl();
        this.netexStopEntitiesIndex = new NetexEntitiesIndexImpl();
        this.gtfsDao = new GtfsRelationalDaoImpl();
    }

    public void importNetex() {
        LOGGER.info("Importing NeTEx Timetable dataset");
        try (ZipInputStream zipInputStream = new ZipInputStream(timetableDataset)) {
            parse(netexParser, zipInputStream, netexTimetableEntitiesIndex);
        } catch (IOException e) {
            throw new GtfsImportException("Error while loading the NeTEx timetable dataset", e);
        }
        LOGGER.info("Importing NeTEx Stop dataset");
        try (ZipInputStream zipInputStream = new ZipInputStream(stopDataset)) {
            parse(netexParser, zipInputStream, netexStopEntitiesIndex);
        } catch (IOException e) {
            throw new GtfsImportException("Error while loading the NeTEx stop dataset", e);
        }
    }

    public void convertNetexToGtfs() {
        LOGGER.info("Converting NeTEx to GTFS");
        String timeZone = netexTimetableEntitiesIndex.getCompositeFrames()
                .stream()
                .findFirst()
                .orElseThrow()
                .getFrameDefaults().getDefaultLocale().getTimeZone();

        AgencyProducer agencyProducer = new AgencyProducer(timeZone);
        netexTimetableEntitiesIndex.getAuthorityIndex().getAll().stream().map(agencyProducer::produce).forEach(gtfsDao::saveEntity);
        netexTimetableEntitiesIndex.getOperatorIndex().getAll().stream().map(agencyProducer::produce).forEach(gtfsDao::saveEntity);

        Agency agency = ((GtfsDao) gtfsDao).getAllAgencies().stream().findFirst().orElseThrow();

        Map<String, StopPlace> stopPlaceByQuayId = netexStopEntitiesIndex.getStopPlaceIdByQuayIdIndex()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> netexStopEntitiesIndex.getStopPlaceIndex().getLatestVersion(entry.getValue())));



        StopProducer stopProducer = new StopProducer(agency, stopPlaceByQuayId, timeZone);

        //stopPlaceByQuayId.values().stream().distinct().map(stopProducer::produceStop).forEach(gtfsDao::saveEntity);


        netexTimetableEntitiesIndex.getQuayIdByStopPointRefIndex()
                .values()
                .stream()
                .distinct()
                .map(netexStopEntitiesIndex.getStopPlaceIdByQuayIdIndex()::get)
                .distinct()
                .map(netexStopEntitiesIndex.getStopPlaceIndex()::getLatestVersion)
                .map(stopProducer::produceStop)
                .forEach(gtfsDao::saveEntity);

        netexTimetableEntitiesIndex.getQuayIdByStopPointRefIndex()
                .values()
                .stream()
                .distinct()
                .map(netexStopEntitiesIndex.getQuayIndex()::getLatestVersion)
                .map(stopProducer::produceStop)
                .forEach(gtfsDao::saveEntity);



        RouteProducer routeProducer = new RouteProducer(agency);
        netexTimetableEntitiesIndex.getLineIndex().getAll().stream().map(routeProducer::produce).forEach(gtfsDao::saveEntity);


    }

    public InputStream exportGtfs() {
        LOGGER.info("Exporting GTFS archive");
        GtfsWriter writer = null;
        try {
            File outputFile = File.createTempFile("damu-export-gtfs-", ".zip");
            writer = new GtfsWriter();
            writer.setOutputLocation(outputFile);
            writer.run((GtfsDao) gtfsDao);

            return createDeleteOnCloseInputStream(outputFile);

        } catch (IOException e) {
            throw new GtfsImportException("Error while saving the GTFS dataset", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOGGER.warn("Error while closing the GTFS writer", e);
                }
            }
        }

    }


    private NetexEntitiesIndex parse(NetexParser parser, ZipInputStream zipInputStream, NetexEntitiesIndex index) throws IOException {
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            byte[] allBytes = zipInputStream.readAllBytes();
            parser.parse(new ByteArrayInputStream(allBytes), index);
            zipEntry = zipInputStream.getNextEntry();
        }
        return index;
    }

    /**
     * Open an input stream on a temporary file with the guarantee that the file will be delete when the stream is closed.
     *
     * @param tmpFile
     * @return
     * @throws IOException
     */
    public static InputStream createDeleteOnCloseInputStream(File tmpFile) throws IOException {
        return Files.newInputStream(tmpFile.toPath(), StandardOpenOption.DELETE_ON_CLOSE);
    }
}
