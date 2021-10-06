package no.entur.damu.export.model;

import org.onebusaway.gtfs.model.ShapePoint;

import java.util.List;

public class GtfsShape {

    private final String id;
    private final List<ShapePoint> shapePoints;
    private final List<ShapePoint> lastShapePointsInServiceLink;


    public GtfsShape(String id, List<ShapePoint> shapePoints, List<ShapePoint> lastShapePointsInServiceLink) {
        this.id = id;
        this.shapePoints = shapePoints;
        this.lastShapePointsInServiceLink = lastShapePointsInServiceLink;
    }

    public String getId() {
        return id;
    }

    public List<ShapePoint> getAllShapePoints() {
        return shapePoints;
    }

    public double getDistanceTravelledToServiceLink(int i) {
        return lastShapePointsInServiceLink.get(i).getDistTraveled();
        }
}
