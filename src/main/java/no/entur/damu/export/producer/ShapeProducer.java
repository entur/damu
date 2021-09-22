package no.entur.damu.export.producer;

import no.entur.damu.export.util.GtfsUtil;
import no.entur.damu.export.util.JtsGmlConverter;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ShapePoint;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LinkInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.LinkSequenceProjection;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.ServiceLinkInJourneyPattern_VersionedChildStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShapeProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShapeProducer.class);


    private final Agency agency;
    private final NetexEntitiesIndex netexTimetableEntitiesIndex;
    private final GeometryFactory factory;


    public ShapeProducer(Agency agency, NetexEntitiesIndex netexTimetableEntitiesIndex) {
        this.agency = agency;
        this.netexTimetableEntitiesIndex = netexTimetableEntitiesIndex;
        this.factory = new GeometryFactory(new PrecisionModel(10), 4326);
    }


    public List<ShapePoint> produce(JourneyPattern journeyPattern) {
        int nbStopPoints = journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().size();
        if (journeyPattern.getLinksInSequence() == null
                || journeyPattern.getLinksInSequence().getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern() == null
                || journeyPattern.getLinksInSequence().getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern().size() != (nbStopPoints - 1)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Skipping GTFS shape export for JourneyPattern with incomplete service links: {} ", journeyPattern.getId());
            }
            return Collections.emptyList();
        }

        List<ShapePoint> shapePoints = new ArrayList<>();
        String shapeId = GtfsUtil.toGtfsId(journeyPattern.getId(), null, true);
        int sequence = 0;
        double distanceFromStart = 0;
        Coordinate previousPoint = null;
        for (LinkInLinkSequence_VersionedChildStructure link : journeyPattern.getLinksInSequence().getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern()) {
            ServiceLinkInJourneyPattern_VersionedChildStructure serviceLinkInJourneyPattern = (ServiceLinkInJourneyPattern_VersionedChildStructure) link;
            ServiceLink serviceLink = netexTimetableEntitiesIndex.getServiceLinkIndex().get(serviceLinkInJourneyPattern.getServiceLinkRef().getRef());
            for (JAXBElement<?> jaxbElement : serviceLink.getProjections().getProjectionRefOrProjection()) {
                LinkSequenceProjection linkSequenceProjection = (LinkSequenceProjection) jaxbElement.getValue();
                LineString lineString = JtsGmlConverter.fromGmlToJts(linkSequenceProjection.getLineString());
                for (Coordinate currentPoint : lineString.getCoordinates()) {
                    // the first point of the current link is the last point of the previous link, it can be skipped.
                    if (currentPoint.equals(previousPoint)) {
                        continue;
                    }
                    distanceFromStart += computeDistance(previousPoint, currentPoint);
                    ShapePoint shapePoint = new ShapePoint();
                    AgencyAndId agencyAndId = new AgencyAndId();
                    agencyAndId.setId(shapeId);
                    agencyAndId.setAgencyId(agency.getId());
                    shapePoint.setShapeId(agencyAndId);
                    shapePoint.setSequence(sequence);
                    shapePoint.setLon(currentPoint.getX());
                    shapePoint.setLat(currentPoint.getY());
                    shapePoint.setDistTraveled(distanceFromStart);
                    shapePoints.add(shapePoint);
                    sequence++;
                    previousPoint = currentPoint;
                }
            }
        }
        return shapePoints;
    }

    private double computeDistance(Coordinate from, Coordinate to) {
        if (from == null) {
            return 0;
        }
        LineString ls = factory.createLineString(new Coordinate[]{from, to});
        return ls.getLength() * (Math.PI / 180) * 6378137;
    }

}
