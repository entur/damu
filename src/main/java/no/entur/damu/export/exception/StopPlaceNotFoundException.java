package no.entur.damu.export.exception;

/**
 * Exception triggered when a referenced stop place cannot be found in the stop area repository.
 */
public class StopPlaceNotFoundException extends GtfsExportException {
    public StopPlaceNotFoundException(String message) {
        super(message);
    }
}
