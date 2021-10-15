package no.entur.damu.export.exception;

/**
 * Exception thrown when an Authority does not have a valid URL.
 */
public class MissingAuthorityUrlException extends GtfsExportException {
    public MissingAuthorityUrlException(String message) {
        super(message);
    }
}
