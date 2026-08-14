package no.entur.damu.pubsub;

import java.util.Map;

/**
 * Publishes a message and waits for the broker to confirm it.
 */
public interface PubSubPublisher {
  void publish(String destination, String body, Map<String, String> attributes);
}
