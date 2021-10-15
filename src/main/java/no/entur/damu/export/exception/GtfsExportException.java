package no.entur.damu.export.exception;

/**
 * Base class for exceptions that occur during the NeTEx to GTFS export process.
 * These exceptions are caused by inconsistencies in the input NeTEx dataset.
 * The operation is in general not retryable.
 */
public class GtfsExportException extends RuntimeException {

    public GtfsExportException(Throwable cause) {
        super(cause);
    }

    public GtfsExportException(String message) {
        super(message);
    }

    public GtfsExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
