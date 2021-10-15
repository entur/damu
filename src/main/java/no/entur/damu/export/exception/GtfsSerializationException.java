package no.entur.damu.export.exception;

/**
 * Exception when serializing the GTFS in-memory model to a GTFS archive.
 * This can be triggered by a transient IO exception.
 * The operation may be retried.
 */
public class GtfsSerializationException extends RuntimeException {

    public GtfsSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
