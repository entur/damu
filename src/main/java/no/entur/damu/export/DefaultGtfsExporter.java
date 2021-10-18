package no.entur.damu.export;

import no.entur.damu.export.exception.QuayNotFoundException;
import no.entur.damu.export.exception.StopPlaceNotFoundException;
import no.entur.damu.export.loader.DefaultNetexDatasetLoader;
import no.entur.damu.export.loader.NetexDatasetLoader;
import no.entur.damu.export.model.GtfsService;
import no.entur.damu.export.model.GtfsShape;
import no.entur.damu.export.model.ServiceCalendarPeriod;
import no.entur.damu.export.producer.AgencyProducer;
import no.entur.damu.export.producer.DefaultAgencyProducer;
import no.entur.damu.export.producer.DefaultGtfsServiceRepository;
import no.entur.damu.export.producer.DefaultRouteProducer;
import no.entur.damu.export.producer.DefaultServiceCalendarDateProducer;
import no.entur.damu.export.producer.DefaultServiceCalendarProducer;
import no.entur.damu.export.producer.DefaultShapeProducer;
import no.entur.damu.export.producer.DefaultStopProducer;
import no.entur.damu.export.producer.DefaultStopTimeProducer;
import no.entur.damu.export.producer.DefaultTransferProducer;
import no.entur.damu.export.producer.DefaultTripProducer;
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
import no.entur.damu.export.repository.DefaultGtfsRepository;
import no.entur.damu.export.repository.DefaultNetexDatasetRepository;
import no.entur.damu.export.repository.GtfsDatasetRepository;
import no.entur.damu.export.repository.NetexDatasetRepository;
import no.entur.damu.export.stop.StopAreaRepository;
import no.entur.damu.export.util.DestinationDisplayUtil;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FeedInfo;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
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

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DefaultGtfsExporter implements GtfsExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultGtfsExporter.class);

    private final String codespace;

    private final NetexDatasetRepository netexDatasetRepository;
    private final GtfsDatasetRepository gtfsDatasetRepository;
    private final GtfsServiceRepository gtfsServiceRepository;
    private final StopAreaRepository stopAreaRepository;

    private TransferProducer transferProducer;
    private AgencyProducer agencyProducer;
    private FeedInfoProducer feedInfoProducer;
    private RouteProducer routeProducer;
    private ShapeProducer shapeProducer;
    private TripProducer tripProducer;
    private StopTimeProducer stopTimeProducer;
    private ServiceCalendarDateProducer serviceCalendarDateProducer;
    private ServiceCalendarProducer serviceCalendarProducer;
    private StopProducer stopProducer;
    private NetexDatasetLoader netexDatasetLoader;

    public DefaultGtfsExporter(String codespace, StopAreaRepository stopAreaRepository) {

        this.codespace = codespace;

        this.netexDatasetLoader = new DefaultNetexDatasetLoader();

        this.stopAreaRepository = stopAreaRepository;
        this.gtfsDatasetRepository = new DefaultGtfsRepository();
        this.netexDatasetRepository = new DefaultNetexDatasetRepository();
        this.gtfsServiceRepository = new DefaultGtfsServiceRepository(codespace, netexDatasetRepository);

        this.transferProducer = new DefaultTransferProducer(netexDatasetRepository, gtfsDatasetRepository);
        this.agencyProducer = new DefaultAgencyProducer(netexDatasetRepository);
        this.routeProducer = new DefaultRouteProducer(netexDatasetRepository, gtfsDatasetRepository);
        this.shapeProducer = new DefaultShapeProducer(netexDatasetRepository, gtfsDatasetRepository);
        this.tripProducer = new DefaultTripProducer(netexDatasetRepository, gtfsDatasetRepository, gtfsServiceRepository);
        this.stopTimeProducer = new DefaultStopTimeProducer(netexDatasetRepository, gtfsDatasetRepository);
        this.serviceCalendarDateProducer = new DefaultServiceCalendarDateProducer(gtfsDatasetRepository);
        this.serviceCalendarProducer = new DefaultServiceCalendarProducer(gtfsDatasetRepository);
        this.stopProducer = new DefaultStopProducer(stopAreaRepository, gtfsDatasetRepository);

    }

    @Override
    public InputStream convertNetexToGtfs(InputStream netexTimetableDataset) {
        loadNetex(netexTimetableDataset);
        convertNetexToGtfs();
        return gtfsDatasetRepository.writeGtfs();

    }

    private void loadNetex(InputStream netexTimetableDataset) {
        LOGGER.info("Importing NeTEx Timetable dataset");
        netexDatasetLoader.load(netexTimetableDataset, netexDatasetRepository);
        LOGGER.info("Imported NeTEx Timetable dataset");
    }

    private void convertNetexToGtfs() {
        LOGGER.info("Converting NeTEx to GTFS");
        // create agencies only for authorities that are effectively referenced from a NeTex line
        netexDatasetRepository.getLines()
                .stream()
                .map(netexDatasetRepository::getAuthorityIdForLine)
                .distinct()
                .map(netexDatasetRepository::getAuthorityById)
                .map(agencyProducer::produce).forEach(gtfsDatasetRepository::saveEntity);

        convertStops();
        convertRoutes();
        convertServices();
        convertTransfers();
        addFeedInfo();

    }


    protected void addFeedInfo() {
        if(feedInfoProducer != null) {
            FeedInfo feedInfo = feedInfoProducer.produceFeedInfo();
            if(feedInfo != null) {
                gtfsDatasetRepository.saveEntity(feedInfo);
            }
        }
    }

    protected void convertRoutes() {
        for (Line netexLine : netexDatasetRepository.getLines()) {
            Route gtfsRoute = routeProducer.produce(netexLine);
            gtfsDatasetRepository.saveEntity(gtfsRoute);
            for (org.rutebanken.netex.model.Route netexRoute : netexDatasetRepository.getRoutesByLine(netexLine)) {
                for (JourneyPattern journeyPattern : netexDatasetRepository.getJourneyPatternsByRoute(netexRoute)) {
                    GtfsShape gtfsShape = shapeProducer.produce(journeyPattern);
                    AgencyAndId shapeId = null;
                    if (gtfsShape != null && !gtfsShape.getShapePoints().isEmpty()) {
                        gtfsShape.getShapePoints().forEach(gtfsDatasetRepository::saveEntity);
                        shapeId = new AgencyAndId();
                        shapeId.setAgencyId(gtfsDatasetRepository.getDefaultAgency().getId());
                        shapeId.setId(gtfsShape.getId());
                    }

                    DestinationDisplay initialDestinationDisplay = DestinationDisplayUtil.getInitialDestinationDisplay(journeyPattern, netexDatasetRepository);

                    for (ServiceJourney serviceJourney : netexDatasetRepository.getServiceJourneysByJourneyPattern(journeyPattern)) {
                        Trip trip = tripProducer.produce(serviceJourney, netexRoute, gtfsRoute, shapeId, initialDestinationDisplay);
                        if (trip != null) {
                            gtfsDatasetRepository.saveEntity(trip);
                            // the head sign set on a given stop depends on the one set on the previous stop
                            // i.e. it must be repeated from one stop to the next unless there is an explicit change.
                            String currentHeadSign = null;
                            for (TimetabledPassingTime timetabledPassingTime : serviceJourney.getPassingTimes().getTimetabledPassingTime()) {
                                StopTime stopTime = stopTimeProducer.produce(timetabledPassingTime, journeyPattern, trip, gtfsShape, currentHeadSign);
                                gtfsDatasetRepository.saveEntity(stopTime);
                                currentHeadSign = stopTime.getStopHeadsign();
                            }
                        }
                    }
                }
            }
        }
    }

    protected void convertServices() {
        for (GtfsService gtfsService : gtfsServiceRepository.getAllServices()) {
            ServiceCalendarPeriod serviceCalendarPeriod = gtfsService.getServiceCalendarPeriod();
            if (serviceCalendarPeriod != null) {
                ServiceCalendar serviceCalendar = serviceCalendarProducer.produce(gtfsService.getId(), serviceCalendarPeriod.getStartDate(), serviceCalendarPeriod.getEndDate(), serviceCalendarPeriod.getDaysOfWeek());
                gtfsDatasetRepository.saveEntity(serviceCalendar);
            }
            for (LocalDateTime includedDate : gtfsService.getIncludedDates()) {
                gtfsDatasetRepository.saveEntity(serviceCalendarDateProducer.produce(gtfsService.getId(), includedDate, true));
            }
            for (LocalDateTime excludedDate : gtfsService.getExcludedDates()) {
                gtfsDatasetRepository.saveEntity(serviceCalendarDateProducer.produce(gtfsService.getId(), excludedDate, false));
            }
        }
    }

    protected void convertTransfers() {
        netexDatasetRepository.getServiceJourneyInterchanges()
                .stream()
                .map(transferProducer::produce)
                .forEach(gtfsDatasetRepository::saveEntity);
    }

    protected void convertStops() {
        // Retrieve all quays referenced by valid ServiceJourneys
        // This excludes quays referenced by cancelled or replaced service journeys
        // and quays referenced only as route points or in dead runs
        Set<String> allQuaysId = netexDatasetRepository.getServiceJourneys()
                .stream()
                .filter(serviceJourney -> ServiceAlterationEnumeration.CANCELLATION != serviceJourney.getServiceAlteration()
                        && ServiceAlterationEnumeration.REPLACED != serviceJourney.getServiceAlteration())
                .map(serviceJourney -> serviceJourney.getJourneyPatternRef().getValue().getRef())
                .distinct()
                .map(netexDatasetRepository::getJourneyPatternById)
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
                .forEach(gtfsDatasetRepository::saveEntity);

        // Retrieve and persist all the stop places that contain the quays
        allQuaysId.stream()
                .map(this::findStopPlaceByQuayId)
                .distinct()
                .map(stopProducer::produceStopFromStopPlace)
                .forEach(gtfsDatasetRepository::saveEntity);
    }

    private boolean isFlexibleScheduledStopPoint(String scheduledStopPointId) {
        String flexibleStopPlaceId = netexDatasetRepository.getFlexibleStopPlaceIdByScheduledStopPointId(scheduledStopPointId);
        if (flexibleStopPlaceId != null) {
            LOGGER.warn("Ignoring scheduled stop point {} referring to flexible stop place {}", scheduledStopPointId, flexibleStopPlaceId);
            return true;
        }
        return false;
    }

    private String findQuayIdByScheduledStopPointId(String scheduledStopPointRef) {
        String quayId = netexDatasetRepository.getQuayIdByScheduledStopPointId(scheduledStopPointRef);
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


    protected final String getCodespace() {
        return codespace;
    }

    protected final NetexDatasetRepository getNetexDatasetRepository() {
        return netexDatasetRepository;
    }

    protected final GtfsDatasetRepository getGtfsDatasetRepository() {
        return gtfsDatasetRepository;
    }

    protected final GtfsServiceRepository getGtfsServiceRepository() {
        return gtfsServiceRepository;
    }

    protected final StopAreaRepository getStopAreaRepository() {
        return stopAreaRepository;
    }

    protected final void setNetexDatasetLoader(NetexDatasetLoader netexDatasetLoader) {
        this.netexDatasetLoader = netexDatasetLoader;
    }

    protected final void setTransferProducer(TransferProducer transferProducer) {
        this.transferProducer = transferProducer;
    }

    protected final void setAgencyProducer(AgencyProducer agencyProducer) {
        this.agencyProducer = agencyProducer;
    }

    protected final void setFeedInfoProducer(FeedInfoProducer feedInfoProducer) {
        this.feedInfoProducer = feedInfoProducer;
    }

    protected final void setRouteProducer(RouteProducer routeProducer) {
        this.routeProducer = routeProducer;
    }

    protected final void setShapeProducer(ShapeProducer shapeProducer) {
        this.shapeProducer = shapeProducer;
    }

    protected final void setTripProducer(TripProducer tripProducer) {
        this.tripProducer = tripProducer;
    }

    protected final void setStopTimeProducer(StopTimeProducer stopTimeProducer) {
        this.stopTimeProducer = stopTimeProducer;
    }

    protected final void setServiceCalendarDateProducer(ServiceCalendarDateProducer serviceCalendarDateProducer) {
        this.serviceCalendarDateProducer = serviceCalendarDateProducer;
    }

    protected final void setServiceCalendarProducer(ServiceCalendarProducer serviceCalendarProducer) {
        this.serviceCalendarProducer = serviceCalendarProducer;
    }

    protected final void setStopProducer(StopProducer stopProducer) {
        this.stopProducer = stopProducer;
    }


}
