package no.entur.damu.export;

import no.entur.damu.export.exception.NetexParsingException;
import no.entur.damu.export.exception.QuayNotFoundException;
import no.entur.damu.export.exception.StopPlaceNotFoundException;
import no.entur.damu.export.model.GtfsService;
import no.entur.damu.export.model.GtfsShape;
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
import no.entur.damu.export.serializer.GtfsSerializer;
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
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
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
        return new GtfsSerializer(gtfsDao).writeGtfs();
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
        StopTimeProducer stopTimeProducer = new StopTimeProducer(netexTimetableEntitiesIndex, gtfsDao);
        for (Line netexLine : netexTimetableEntitiesIndex.getLineIndex().getAll()) {
            Route gtfsRoute = routeProducer.produce(netexLine);
            gtfsDao.saveEntity(gtfsRoute);
            for (org.rutebanken.netex.model.Route netexRoute : getNetexRouteForNetexLine(netexLine)) {
                for (JourneyPattern journeyPattern : getJourneyPatternForNetexRoute(netexRoute)) {
                    GtfsShape gtfsShape = shapeProducer.produce(journeyPattern);
                    AgencyAndId shapeId = null;
                    if (gtfsShape != null && !gtfsShape.getShapePoints().isEmpty()) {
                        gtfsShape.getShapePoints().forEach(gtfsDao::saveEntity);
                        shapeId = new AgencyAndId();
                        shapeId.setAgencyId(agency.getId());
                        shapeId.setId(gtfsShape.getId());
                    }

                    StopPointInJourneyPattern firstStopPointInJourneyPattern = (StopPointInJourneyPattern) journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(0);
                    DestinationDisplay startDestinationDisplay = netexTimetableEntitiesIndex.getDestinationDisplayIndex().get(firstStopPointInJourneyPattern.getDestinationDisplayRef().getRef());

                    for (ServiceJourney serviceJourney : getServiceJourneyForJourneyPattern(journeyPattern)) {
                        Trip trip = tripProducer.produce(serviceJourney, journeyPattern, netexRoute, gtfsRoute, shapeId, startDestinationDisplay);
                        if (trip != null) {
                            gtfsDao.saveEntity(trip);
                            String currentHeadSign = null;
                            for (TimetabledPassingTime timetabledPassingTime : serviceJourney.getPassingTimes().getTimetabledPassingTime()) {
                                StopTime stopTime = stopTimeProducer.produce(timetabledPassingTime, journeyPattern, trip, gtfsShape, currentHeadSign);
                                gtfsDao.saveEntity(stopTime);
                                currentHeadSign = stopTime.getStopHeadsign();
                            }
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

        // Retrieve all quays referenced by valid ServiceJourneys
        // This excludes quays referenced by cancelled or replaced service journeys
        // and quays referenced only as route points or in dead runs
        Set<String> allQuaysId = netexTimetableEntitiesIndex.getServiceJourneyIndex()
                .getAll()
                .stream()
                .filter(serviceJourney -> ServiceAlterationEnumeration.CANCELLATION != serviceJourney.getServiceAlteration()
                        && ServiceAlterationEnumeration.REPLACED != serviceJourney.getServiceAlteration())
                .map(serviceJourney -> serviceJourney.getJourneyPatternRef().getValue().getRef())
                .distinct()
                .map(journeyPatternRef -> netexTimetableEntitiesIndex.getJourneyPatternIndex().get(journeyPatternRef))
                .map(journeyPattern -> journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern())
                .flatMap(Collection::stream)
                .map(stopPointInJourneyPattern -> ((StopPointInJourneyPattern) stopPointInJourneyPattern).getScheduledStopPointRef().getValue().getRef())
                .distinct()
                .filter(Predicate.not(this::isFlexibleScheduledStopPoint))
                .map(this::findQuayIdByScheduledStopPointId)
                .collect(Collectors.toSet());

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

    private boolean isFlexibleScheduledStopPoint(String scheduledStopPointId) {
        String flexibleStopPlaceId = netexTimetableEntitiesIndex.getFlexibleStopPlaceIdByStopPointRefIndex().get(scheduledStopPointId);
        if (flexibleStopPlaceId != null) {
            LOGGER.warn("Ignoring scheduled stop point {} referring to flexible stop place {}", scheduledStopPointId, flexibleStopPlaceId);
            return true;
        }
        return false;
    }

    private String findQuayIdByScheduledStopPointId(String scheduledStopPointRef) {
        String quayId = netexTimetableEntitiesIndex.getQuayIdByStopPointRefIndex().get(scheduledStopPointRef);
        if (quayId == null) {
            throw new QuayNotFoundException("Could not find Quay id for scheduled stop point id " + scheduledStopPointRef);
        }
        return quayId;
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


}
