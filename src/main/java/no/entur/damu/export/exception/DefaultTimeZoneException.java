
package no.entur.damu.export.exception;

/**
 * Exception thrown when the NeTEx dataset default timezone cannot be found.
 */
public class DefaultTimeZoneException extends GtfsExportException {
    public DefaultTimeZoneException(String message) {
        super(message);
    }
}
