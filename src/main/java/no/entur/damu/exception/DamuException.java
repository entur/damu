package no.entur.damu.exception;

/**
 * Thrown when damu cannot finish the work it was asked to do. Escaping the consumer callback nacks the
 * message, so PubSub redelivers it.
 */
public class DamuException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DamuException(String message) {
    super(message);
  }

  public DamuException(String message, Throwable cause) {
    super(message, cause);
  }
}
