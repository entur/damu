package no.entur.damu.export.exception;

/**
 * Exception triggered when the NeTEx dataset cannot be loaded into an in-memory object model.
 * This can be caused by a transient IO exception.
 * The operation may be retried.
 */
public class NetexParsingException extends RuntimeException {

    public NetexParsingException(String message, Throwable cause) {
        super(message, cause);
    }

}
