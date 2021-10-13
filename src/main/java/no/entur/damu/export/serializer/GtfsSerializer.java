package no.entur.damu.export.serializer;

import java.io.InputStream;

public interface GtfsSerializer {
    InputStream writeGtfs();
}
