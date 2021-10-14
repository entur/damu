package no.entur.damu.export;

import java.io.InputStream;

public interface GtfsExporter {

    /**
     * Convert a Netex timetable dataset into a GTFS dataset.
     * @param netexTimetableDataset a ZIP archive containing a NeTEx timetable dataset.
     * @return a ZIP archive containing a GTFS dataset.
     */
    InputStream convertNetexToGtfs(InputStream netexTimetableDataset);
}
