package no.entur.damu.exception;

public class GtfsImportException extends RuntimeException {
    public GtfsImportException(Exception e) {
        super(e);
    }

    public GtfsImportException(String message, Exception e) {
        super(message, e);
    }
}
