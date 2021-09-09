package no.entur.damu.exception;

public class DamuException extends RuntimeException {

    public DamuException(Throwable cause) {
        super(cause);
    }

    public DamuException(String message) {
        super(message);
    }

    public DamuException(String message, Throwable cause) {
        super(message, cause);
    }
}
