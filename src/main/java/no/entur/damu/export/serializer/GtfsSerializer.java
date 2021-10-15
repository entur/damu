package no.entur.damu.export.serializer;

import java.io.InputStream;

/**
 * Serialize a GTFS dataset into a zip archive.
 */
public interface GtfsSerializer {
    InputStream writeGtfs();
}
