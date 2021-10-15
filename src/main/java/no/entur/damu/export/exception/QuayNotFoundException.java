package no.entur.damu.export.exception;

/**
 * Exception triggered when a referenced quay cannot be found in the stop area repository.
 */
public class QuayNotFoundException extends GtfsExportException {
    public QuayNotFoundException(String message) {
        super(message);
    }
}
