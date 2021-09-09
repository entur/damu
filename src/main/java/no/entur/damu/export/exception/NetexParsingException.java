package no.entur.damu.export.exception;

public class NetexParsingException extends GtfsExportException {

    public NetexParsingException(Throwable cause) {
        super(cause);
    }

    public NetexParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
