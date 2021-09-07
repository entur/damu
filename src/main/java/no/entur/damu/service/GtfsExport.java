package no.entur.damu.service;

import no.entur.damu.exception.DamuException;
import no.entur.damu.exception.GtfsImportException;
import no.entur.damu.producer.AgencyProducer;
import no.entur.damu.producer.FeedInfoProducer;
import no.entur.damu.producer.GtfsServiceRepository;
import no.entur.damu.producer.RouteProducer;
import no.entur.damu.producer.StopProducer;
import no.entur.damu.producer.StopTimeProducer;
import no.entur.damu.producer.TripProducer;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.serialization.GtfsWriter;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GtfsExport {

    private static final Logger LOGGER = LoggerFactory.getLogger(GtfsExport.class);

    private final NetexParser netexParser;
    private final NetexEntitiesIndex netexTimetableEntitiesIndex;
    private final NetexEntitiesIndex netexStopEntitiesIndex;
    private final GtfsMutableDao gtfsDao;
    private final GtfsServiceRepository gtfsServiceRepository;

    private final InputStream timetableDataset;
    private final InputStream stopDataset;

    public GtfsExport(InputStream timetableDataset, InputStream stopDataset) {
        this.timetableDataset = timetableDataset;
        this.stopDataset = stopDataset;
        this.netexParser = new NetexParser();
        this.netexTimetableEntitiesIndex = new NetexEntitiesIndexImpl();
        this.netexStopEntitiesIndex = new NetexEntitiesIndexImpl();
        this.gtfsDao = new GtfsRelationalDaoImpl();
        this.gtfsServiceRepository = new GtfsServiceRepository();
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

        Agency agency = gtfsDao.getAllAgencies().stream().findFirst().orElseThrow();
        convertStops();
        convertRoutes(agency);
        addFeedInfo();

    }

    private void addFeedInfo() {
        FeedInfoProducer feedInfoProducer = new FeedInfoProducer();
        gtfsDao.saveEntity(feedInfoProducer.produceFeedInfo());
    }

    private void convertRoutes(Agency agency) {
        RouteProducer routeProducer = new RouteProducer(agency);
        for (Line netexLine : netexTimetableEntitiesIndex.getLineIndex().getAll()) {
            Route gtfsRoute = routeProducer.produce(netexLine);
            gtfsDao.saveEntity(gtfsRoute);
            for (org.rutebanken.netex.model.Route netexRoute : getNetexRouteForNetexLine(netexLine)) {
                for (JourneyPattern journeyPattern : getJourneyPatternForNetexRoute(netexRoute)) {
                    for (ServiceJourney serviceJourney : getServiceJourneyForJourneyPattern(journeyPattern)) {
                        TripProducer tripProducer = new TripProducer(agency, gtfsRoute, gtfsServiceRepository, netexTimetableEntitiesIndex);
                        Trip trip = tripProducer.produce(serviceJourney);
                        gtfsDao.saveEntity(trip);
                        for(TimetabledPassingTime timetabledPassingTime: serviceJourney.getPassingTimes().getTimetabledPassingTime()) {
                            StopTimeProducer stopTimeProducer = new StopTimeProducer(netexTimetableEntitiesIndex, gtfsDao);
                            StopTime stopTime = stopTimeProducer.produce(timetabledPassingTime, journeyPattern, trip);
                            gtfsDao.saveEntity(stopTime);
                        }
                    }
                }
            }
        }
    }

    private Collection<ServiceJourney> getServiceJourneyForJourneyPattern(JourneyPattern journeyPattern) {
        return netexTimetableEntitiesIndex.getServiceJourneyIndex()
                .getAll()
                .stream()
                .filter(serviceJourney -> serviceJourney.getJourneyPatternRef().getValue().getRef().equals(journeyPattern.getId()))
                .collect(Collectors.toSet());
    }

    private Collection<org.rutebanken.netex.model.Route> getNetexRouteForNetexLine(Line netexLine) {
        return netexTimetableEntitiesIndex.getRouteIndex()
                .getAll()
                .stream()
                .filter(route -> route.getLineRef().getValue().getRef().equals(netexLine.getId()))
                .collect(Collectors.toSet());
    }

    private Collection<JourneyPattern> getJourneyPatternForNetexRoute(org.rutebanken.netex.model.Route netexRoute) {
        return netexTimetableEntitiesIndex.getJourneyPatternIndex()
                .getAll()
                .stream()
                .filter(journeyPattern -> journeyPattern.getRouteRef().getRef().equals(netexRoute.getId()))
                .collect(Collectors.toSet());
    }


    private void convertStops() {

        Map<String, StopPlace> stopPlaceByQuayId = netexStopEntitiesIndex.getStopPlaceIdByQuayIdIndex()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> netexStopEntitiesIndex.getStopPlaceIndex().getLatestVersion(entry.getValue())));


        StopProducer stopProducer = new StopProducer(stopPlaceByQuayId);

        // Retrieve all quay IDs in use in the timetable dataset
        Set<String> allQuaysId = new HashSet<>(netexTimetableEntitiesIndex.getQuayIdByStopPointRefIndex().values());

        // Persist the quays
        allQuaysId.stream().map(this::findQuayById)
                .map(stopProducer::produceStopFromQuay)
                .forEach(gtfsDao::saveEntity);

        // Retrieve and persist all the stop places that contain the quays
        allQuaysId.stream()
                .map(netexStopEntitiesIndex.getStopPlaceIdByQuayIdIndex()::get)
                .distinct()
                .map(netexStopEntitiesIndex.getStopPlaceIndex()::getLatestVersion)
                .map(stopProducer::produceStopFromStopPlace)
                .forEach(gtfsDao::saveEntity);
    }

    private Quay findQuayById(String quayId) {
        Quay quay = netexStopEntitiesIndex.getQuayIndex().getLatestVersion(quayId);
        if (quay == null) {
            throw new DamuException("Could not find Quay for id " + quayId);
        }
        return quay;
    }

    public InputStream exportGtfs() {
        LOGGER.info("Exporting GTFS archive");
        GtfsWriter writer = null;
        try {
            File outputFile = File.createTempFile("damu-export-gtfs-", ".zip");
            writer = new GtfsWriter();
            writer.setOutputLocation(outputFile);
            writer.run(gtfsDao);

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
