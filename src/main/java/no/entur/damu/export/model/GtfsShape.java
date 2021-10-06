package no.entur.damu.export.model;

import org.onebusaway.gtfs.model.ShapePoint;

import java.util.List;

/**
 * A GTFS shape made of a list of shape points.
 *
 */
public class GtfsShape {

    private final String id;
    private final List<ShapePoint> shapePoints;
    private final List<Double> travelledDistanceToStop;


    public GtfsShape(String id, List<ShapePoint> shapePoints, List<Double> travelledDistanceToStop) {
        this.id = id;
        this.shapePoints = shapePoints;
        this.travelledDistanceToStop = travelledDistanceToStop;
    }

    public String getId() {
        return id;
    }

    public List<ShapePoint> getShapePoints() {
        return shapePoints;
    }


    /**
     * Return the distance travelled on the shape from the start to the stop number i.
     * The distance travelled to stop number 1 is 0 meters.
     * @param i sequence number of the stop in the JourneyPattern, starting at 1.
     * @return the distance travelled on the shape from the start to the stop number i, in meters.
     */
    public double getDistanceTravelledToStop(int i) {
        return travelledDistanceToStop.get(i-1);
        }
}
