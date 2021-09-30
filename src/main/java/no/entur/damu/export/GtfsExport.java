package no.entur.damu.export;

import no.entur.damu.export.exception.GtfsWritingException;
import no.entur.damu.export.exception.NetexParsingException;
import no.entur.damu.export.exception.QuayNotFoundException;
import no.entur.damu.export.exception.StopPlaceNotFoundException;
import no.entur.damu.export.model.GtfsService;
import no.entur.damu.export.model.ServiceCalendarPeriod;
import no.entur.damu.export.producer.AgencyProducer;
import no.entur.damu.export.producer.FeedInfoProducer;
import no.entur.damu.export.producer.GtfsServiceRepository;
import no.entur.damu.export.producer.RouteProducer;
import no.entur.damu.export.producer.ServiceCalendarDateProducer;
import no.entur.damu.export.producer.ServiceCalendarProducer;
import no.entur.damu.export.producer.ShapeProducer;
import no.entur.damu.export.producer.StopProducer;
import no.entur.damu.export.producer.StopTimeProducer;
import no.entur.damu.export.producer.TransferProducer;
import no.entur.damu.export.producer.TripProducer;
import no.entur.damu.export.stop.StopAreaRepository;
import no.entur.damu.export.util.NetexDatasetParserUtil;
import no.entur.damu.export.util.StopUtil;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.serialization.GtfsWriter;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.DestinationDisplayRefStructure;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

public class GtfsExport {

    private static final Logger LOGGER = LoggerFactory.getLogger(GtfsExport.class);

    private final NetexParser netexParser;
    private final NetexEntitiesIndex netexTimetableEntitiesIndex;
    private final GtfsMutableDao gtfsDao;
    private final GtfsServiceRepository gtfsServiceRepository;

    private final InputStream timetableDataset;
    private final StopAreaRepository stopAreaRepository;

    public GtfsExport(String codespace, InputStream timetableDataset, StopAreaRepository stopAreaRepository) {
        this.timetableDataset = timetableDataset;
        this.stopAreaRepository = stopAreaRepository;
        this.netexParser = new NetexParser();
        this.netexTimetableEntitiesIndex = new NetexEntitiesIndexImpl();
        this.gtfsDao = new GtfsRelationalDaoImpl();
        this.gtfsServiceRepository = new GtfsServiceRepository(codespace, netexTimetableEntitiesIndex);
    }

    public InputStream exportGtfs() {
        importNetex();
        convertNetexToGtfs();
        return writeGtfs();
    }

    private void importNetex() {
        LOGGER.info("Importing NeTEx Timetable dataset");
        try (ZipInputStream zipInputStream = new ZipInputStream(timetableDataset)) {
            NetexDatasetParserUtil.parse(netexParser, zipInputStream, netexTimetableEntitiesIndex);
        } catch (IOException e) {
            throw new NetexParsingException("Error while parsing the NeTEx timetable dataset", e);
        }
    }

    private void convertNetexToGtfs() {
        LOGGER.info("Converting NeTEx to GTFS");

        String timeZone = netexTimetableEntitiesIndex.getCompositeFrames()
                .stream()
                .findFirst()
                .orElseThrow()
                .getFrameDefaults().getDefaultLocale().getTimeZone();

        AgencyProducer agencyProducer = new AgencyProducer(timeZone);
        netexTimetableEntitiesIndex.getAuthorityIndex().getAll().stream().map(agencyProducer::produce).forEach(gtfsDao::saveEntity);

        Agency agency = StopUtil.createEnturAgency();
        convertStops();
        convertRoutes(agency);
        convertServices(agency);
        convertTransfers(agency);
        addFeedInfo();

    }


    private void addFeedInfo() {
        FeedInfoProducer feedInfoProducer = new FeedInfoProducer();
        gtfsDao.saveEntity(feedInfoProducer.produceFeedInfo());
    }

    private void convertRoutes(Agency agency) {
        RouteProducer routeProducer = new RouteProducer(netexTimetableEntitiesIndex, gtfsDao);
        ShapeProducer shapeProducer = new ShapeProducer(agency, netexTimetableEntitiesIndex);
        TripProducer tripProducer = new TripProducer(agency, gtfsServiceRepository, netexTimetableEntitiesIndex);
        for (Line netexLine : netexTimetableEntitiesIndex.getLineIndex().getAll()) {
            Route gtfsRoute = routeProducer.produce(netexLine);
            gtfsDao.saveEntity(gtfsRoute);
            for (org.rutebanken.netex.model.Route netexRoute : getNetexRouteForNetexLine(netexLine)) {
                for (JourneyPattern journeyPattern : getJourneyPatternForNetexRoute(netexRoute)) {
                    List<ShapePoint> shapePoints = shapeProducer.produce(journeyPattern);
                    AgencyAndId shapeId = null;
                    if (!shapePoints.isEmpty()) {
                        shapePoints.forEach(gtfsDao::saveEntity);
                        shapeId = new AgencyAndId();
                        shapeId.setAgencyId(agency.getId());
                        shapeId.setId(journeyPattern.getId());
                    }

                    List<DestinationDisplayRefStructure> allDestinationDisplays = journeyPattern.getPointsInSequence()
                            .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                            .stream()
                            .map(sp -> ((StopPointInJourneyPattern) sp).getDestinationDisplayRef()).collect(Collectors.toList());

                    boolean multipleDestinationDisplays = allDestinationDisplays.stream()
                            .filter(Objects::nonNull)
                            .count() > 1;

                    DestinationDisplay startDestinationDisplay = netexTimetableEntitiesIndex.getDestinationDisplayIndex().get(allDestinationDisplays.get(0).getRef());

                    for (ServiceJourney serviceJourney : getServiceJourneyForJourneyPattern(journeyPattern)) {
                        Trip trip = tripProducer.produce(serviceJourney, journeyPattern, netexRoute, gtfsRoute, shapeId, startDestinationDisplay);
                        gtfsDao.saveEntity(trip);
                        for (TimetabledPassingTime timetabledPassingTime : serviceJourney.getPassingTimes().getTimetabledPassingTime()) {
                            StopTimeProducer stopTimeProducer = new StopTimeProducer(netexTimetableEntitiesIndex, gtfsDao);
                            StopTime stopTime = stopTimeProducer.produce(timetabledPassingTime, journeyPattern, trip, multipleDestinationDisplays);
                            gtfsDao.saveEntity(stopTime);
                        }
                    }
                }
            }
        }
    }

    private void convertServices(Agency agency) {
        ServiceCalendarDateProducer serviceCalendarDateProducer = new ServiceCalendarDateProducer(agency);
        ServiceCalendarProducer serviceCalendarProducer = new ServiceCalendarProducer(agency);
        for (GtfsService gtfsService : gtfsServiceRepository.getAllServices()) {
            ServiceCalendarPeriod serviceCalendarPeriod = gtfsService.getServiceCalendarPeriod();
            if (serviceCalendarPeriod != null) {
                ServiceCalendar serviceCalendar = serviceCalendarProducer.produce(gtfsService.getId(), serviceCalendarPeriod.getStartDate(), serviceCalendarPeriod.getEndDate(), serviceCalendarPeriod.getDaysOfWeek());
                gtfsDao.saveEntity(serviceCalendar);
            }
            for (LocalDateTime includedDate : gtfsService.getIncludedDates()) {
                gtfsDao.saveEntity(serviceCalendarDateProducer.produce(gtfsService.getId(), includedDate, true));
            }
            for (LocalDateTime excludedDate : gtfsService.getExcludedDates()) {
                gtfsDao.saveEntity(serviceCalendarDateProducer.produce(gtfsService.getId(), excludedDate, false));
            }

        }

    }

    private void convertTransfers(Agency agency) {
        TransferProducer transferProducer = new TransferProducer(agency, netexTimetableEntitiesIndex, gtfsDao);
        netexTimetableEntitiesIndex.getServiceJourneyInterchangeIndex()
                .getAll()
                .stream()
                .map(transferProducer::produce)
                .forEach(gtfsDao::saveEntity);
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

        StopProducer stopProducer = new StopProducer(stopAreaRepository);

        // Retrieve all quay IDs in use in the timetable dataset
        Set<String> allQuaysId = new HashSet<>(netexTimetableEntitiesIndex.getQuayIdByStopPointRefIndex().values());

        // Persist the quays
        allQuaysId.stream().map(this::findQuayById)
                .map(stopProducer::produceStopFromQuay)
                .forEach(gtfsDao::saveEntity);

        // Retrieve and persist all the stop places that contain the quays
        allQuaysId.stream()
                .map(this::findStopPlaceByQuayId)
                .distinct()
                .map(stopProducer::produceStopFromStopPlace)
                .forEach(gtfsDao::saveEntity);
    }

    private Quay findQuayById(String quayId) {
        Quay quay = stopAreaRepository.getQuayById(quayId);
        if (quay == null) {
            throw new QuayNotFoundException("Could not find Quay for id " + quayId);
        }
        return quay;
    }

    private StopPlace findStopPlaceByQuayId(String quayId) {
        StopPlace stopPlace = stopAreaRepository.getStopPlaceByQuayId(quayId);
        if (stopPlace == null) {
            throw new StopPlaceNotFoundException("Could not find Quay for id " + quayId);
        }
        return stopPlace;
    }

    private InputStream writeGtfs() {
        LOGGER.info("Exporting GTFS archive");
        GtfsWriter writer = null;
        try {
            File outputFile = File.createTempFile("damu-export-gtfs-", ".zip");
            writer = new GtfsWriter();
            writer.setOutputLocation(outputFile);
            writer.run(gtfsDao);

            return createDeleteOnCloseInputStream(outputFile);

        } catch (IOException e) {
            throw new GtfsWritingException("Error while saving the GTFS dataset", e);
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

    /**
     * Open an input stream on a temporary file with the guarantee that the file will be deleted when the stream is closed.
     *
     * @param tmpFile
     * @return
     * @throws IOException
     */
    public static InputStream createDeleteOnCloseInputStream(File tmpFile) throws IOException {
        return Files.newInputStream(tmpFile.toPath(), StandardOpenOption.DELETE_ON_CLOSE);
    }
}
