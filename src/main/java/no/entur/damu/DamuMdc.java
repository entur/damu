package no.entur.damu;

import org.slf4j.MDC;

/**
 * The two fields {@code logback.xml} renders on every line. Each entry point sets them once it knows
 * them.
 */
public final class DamuMdc {

  public static final String CORRELATION_ID_KEY = "correlationId";
  public static final String CODESPACE_KEY = "codespace";

  private DamuMdc() {}

  public static void setCorrelationId(String correlationId) {
    put(CORRELATION_ID_KEY, correlationId);
  }

  public static void setCodespace(String codespace) {
    put(CODESPACE_KEY, codespace);
  }

  public static void clear() {
    MDC.remove(CORRELATION_ID_KEY);
    MDC.remove(CODESPACE_KEY);
  }

  private static void put(String key, String value) {
    if (value == null || value.isEmpty()) {
      MDC.remove(key);
    } else {
      MDC.put(key, value);
    }
  }
}
