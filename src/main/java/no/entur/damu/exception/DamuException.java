package no.entur.damu.exception;

public class DamuException extends RuntimeException {
    public DamuException(Exception e) {
        super(e);
    }
}
