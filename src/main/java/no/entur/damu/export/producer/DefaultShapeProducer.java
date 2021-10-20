/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *  *
 *
 */

package no.entur.damu.export.producer;

import no.entur.damu.export.model.GtfsShape;
import no.entur.damu.export.repository.GtfsDatasetRepository;
import no.entur.damu.export.repository.NetexDatasetRepository;
import no.entur.damu.export.util.JtsGmlConverter;
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
import org.rutebanken.netex.model.Projections_RelStructure;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.ServiceLinkInJourneyPattern_VersionedChildStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

public class DefaultShapeProducer implements ShapeProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultShapeProducer.class);

    /**
     * Earth radius on the equator for the WGS84 system, in meters.
     */
    private static final int EQUATORIAL_RADIUS = 6378137;


    private final Agency agency;
    private final NetexDatasetRepository netexDatasetRepository;
    private final GeometryFactory factory;


    public DefaultShapeProducer(NetexDatasetRepository netexDatasetRepository, GtfsDatasetRepository gtfsDatasetRepository) {
        this.agency = gtfsDatasetRepository.getDefaultAgency();
        this.netexDatasetRepository = netexDatasetRepository;
        this.factory = new GeometryFactory(new PrecisionModel(10), 4326);
    }

    @Override
    public GtfsShape produce(JourneyPattern journeyPattern) {
        int nbStopPoints = journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().size();
        if (journeyPattern.getLinksInSequence() == null
                || journeyPattern.getLinksInSequence().getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern() == null
                || journeyPattern.getLinksInSequence().getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern().size() != (nbStopPoints - 1)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Skipping GTFS shape export for JourneyPattern {} with incomplete list of service links", journeyPattern.getId());
            }
            return null;
        }
        List<ShapePoint> shapePoints = new ArrayList<>();
        List<Double> travelledDistanceToStop = new ArrayList<>(nbStopPoints);
        // distance travelled to first stop is 0.
        travelledDistanceToStop.add(0.0);
        String shapeId = journeyPattern.getId();
        int sequence = 0;
        double distanceFromStart = 0;
        Coordinate previousPoint = null;
        for (LinkInLinkSequence_VersionedChildStructure link : journeyPattern.getLinksInSequence().getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern()) {
            ServiceLinkInJourneyPattern_VersionedChildStructure serviceLinkInJourneyPattern = (ServiceLinkInJourneyPattern_VersionedChildStructure) link;
            ServiceLink serviceLink = netexDatasetRepository.getServiceLinkById(serviceLinkInJourneyPattern.getServiceLinkRef().getRef());
            Projections_RelStructure projections = serviceLink.getProjections();
            if (projections == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Skipping GTFS shape export for JourneyPattern {} with service link {} without LineString", journeyPattern.getId(), serviceLink.getId());
                }
                return null;
            }
            for (JAXBElement<?> jaxbElement : projections.getProjectionRefOrProjection()) {
                LinkSequenceProjection linkSequenceProjection = (LinkSequenceProjection) jaxbElement.getValue();
                LineString lineString = JtsGmlConverter.fromGmlToJts(linkSequenceProjection.getLineString());
                for (Coordinate currentPoint : lineString.getCoordinates()) {
                    // the first point of the current link is the last point of the previous link, it can be skipped.
                    // as a side effect, duplicate points that follow one another are also filtered out
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
                    shapePoint.setDistTraveled(Math.round(distanceFromStart));
                    shapePoints.add(shapePoint);
                    sequence++;
                    previousPoint = currentPoint;
                }
            }
            travelledDistanceToStop.add((double) Math.round(distanceFromStart));
        }
        return new GtfsShape(shapeId, shapePoints, travelledDistanceToStop);
    }

    /**
     * Calculate the distance between 2 coordinates, in meters.
     * @param from from coordinate
     * @param to to coordinate
     * @return the distance between the 2 coordinates, in meters.
     */
    private double computeDistance(Coordinate from, Coordinate to) {
        if (from == null) {
            return 0;
        }
        LineString ls = factory.createLineString(new Coordinate[]{from, to});
        return ls.getLength() * (Math.PI / 180) * EQUATORIAL_RADIUS;
    }

}
