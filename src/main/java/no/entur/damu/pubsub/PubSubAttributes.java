package no.entur.damu.pubsub;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Message attribute handling shared by the consumer and the status notifications.
 */
public final class PubSubAttributes {

  /**
   * The prefix PubSub reserves for itself, and the same one the Camel producer refused to republish
   * ({@code GooglePubsubConstants.RESERVED_GOOGLE_CLIENT_ATTRIBUTE_PREFIX}). Echoing one back would be
   * rejected on publish with INVALID_ARGUMENT.
   */
  static final String RESERVED_GOOGLE_CLIENT_PREFIX = "goog";

  /**
   * Set by the PubSub client on subscriptions that have a dead letter policy. One-based: the first
   * delivery is attempt 1.
   */
  static final String DELIVERY_ATTEMPT = "googclient_deliveryattempt";

  private PubSubAttributes() {}

  /**
   * The inbound attributes, ready to be sent back to marduk. Damu echoes everything the request came
   * with, which is how the correlation id, the provider ids and the referential survive the round trip.
   */
  public static Map<String, String> echo(Map<String, String> inbound) {
    Map<String, String> echoed = new LinkedHashMap<>();
    inbound.forEach((key, value) -> {
      if (!key.startsWith(RESERVED_GOOGLE_CLIENT_PREFIX)) {
        echoed.put(key, value);
      }
    });
    return echoed;
  }

  /**
   * The delivery attempt counter, or 0 when the subscription has no dead letter policy and the client
   * therefore sets none.
   */
  public static int deliveryAttempt(Map<String, String> attributes) {
    String value = attributes.get(DELIVERY_ATTEMPT);
    return value == null ? 0 : Integer.parseInt(value);
  }
}
