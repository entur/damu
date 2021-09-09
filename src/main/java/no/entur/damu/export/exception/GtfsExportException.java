package no.entur.damu.export.exception;

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
