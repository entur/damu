package no.entur.damu.export.util;

import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.LineStringType;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Convert GML LineString into JTS LineString.
 */
public final class JtsGmlConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JtsGmlConverter.class);

    private static final String DEFAULT_SRID_NAME = "WGS84";
    private static final String DEFAULT_SRID_AS_STRING = "4326";
    private static final int DEFAULT_SRID_AS_INT = 4326;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), DEFAULT_SRID_AS_INT);

    private JtsGmlConverter() {
    }

    /**
     * Convert a GML LIneString object into a JTS LineString.
     * @param gml the GML LineString.
     * @return the JTS LineString.
     */
    public static LineString fromGmlToJts(LineStringType gml) {
        List<Double> coordinateList;
        DirectPositionListType posList = gml.getPosList();
        if (posList != null && !posList.getValue().isEmpty()) {
            coordinateList = posList.getValue();
        } else {
            if (gml.getPosOrPointProperty() != null && !gml.getPosOrPointProperty().isEmpty()) {
                coordinateList = new ArrayList<>();
                for (Object o : gml.getPosOrPointProperty()) {
                    if (o instanceof DirectPositionType) {
                        DirectPositionType directPositionType = (DirectPositionType) o;
                        coordinateList.addAll(directPositionType.getValue());
                    } else {
                        LOGGER.warn("Got unrecognized class ({}) for PosOrPointProperty for gmlString {}", o.getClass(), gml.getId());
                    }
                }
                if (coordinateList.isEmpty()) {
                    LOGGER.warn("No recognized class in PosOrPointProperty for gmlString {}", gml.getId());
                    return null;
                }

            } else {
                LOGGER.warn("Got LineStringType without posList or PosOrPointProperty {}", gml.getId());
                return null;
            }
        }


        CoordinateSequence coordinateSequence = convert(coordinateList);
        LineString jts = new LineString(coordinateSequence, GEOMETRY_FACTORY);
        assignSRID(gml, jts);

        return jts;
    }

    /**
     * Assign an SRID to the LineString based on the provided Spatial Reference System name.
     * The LineString is expected to be based on the WGS84 spatial reference system (SRID=4326).
     * If srsName is not set, the SRID defaults to 4326 (default value set by the {@link GeometryFactory}).
     * If srsName is set to either "4326" or "WGS84", the SRID defaults to 4326.
     * If srsName is set to another value, an attempt is made to parse it as a SRID.
     * If srsName is not parseable as a SRID, then the SRID defaults to 4326.
     **/
    private static void assignSRID(LineStringType gml, LineString jts) {
        String srsName = gml.getSrsName();
        if (!StringUtils.isEmpty(srsName) && !DEFAULT_SRID_NAME.equals(srsName) && !DEFAULT_SRID_AS_STRING.equals(srsName)) {
            LOGGER.warn("The LineString {} is not based on the WGS84 Spatial Reference System. SRID in use: {}", gml.getId(), srsName);
            try {
                jts.setSRID(Integer.parseInt(srsName));
            } catch (NumberFormatException nfe) {
                LOGGER.warn("Ignoring SRID on linestring {} for illegal value: {}", gml.getId(), srsName);
            }
        }
    }


    /**
     * Convert a list of double values into a sequence of coordinates.
     * @param values the list of coordinate.
     * @return a coordinate sequence.
     */
    private static CoordinateSequence convert(List<Double> values) {
        Coordinate[] coordinates = new Coordinate[values.size() / 2];
        int coordinateIndex = 0;
        for (int index = 0; index < values.size(); index += 2) {
            Coordinate coordinate = new Coordinate(values.get(index + 1), values.get(index));
            coordinates[coordinateIndex++] = coordinate;
        }
        return new CoordinateArraySequence(coordinates);
    }



}
