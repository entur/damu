package no.entur.damu;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import no.entur.damu.pubsub.PubSubPublisher;

/**
 * Collects what damu would have published, so the pipeline tests can assert on it without a broker.
 */
public class RecordingPubSubPublisher implements PubSubPublisher {

  public record Published(
    String destination,
    String body,
    Map<String, String> attributes
  ) {}

  private final List<Published> published = new CopyOnWriteArrayList<>();

  @Override
  public void publish(
    String destination,
    String body,
    Map<String, String> attributes
  ) {
    published.add(new Published(destination, body, Map.copyOf(attributes)));
  }

  public List<Published> publishedTo(String destination) {
    return published
      .stream()
      .filter(message -> message.destination().equals(destination))
      .toList();
  }

  public void reset() {
    published.clear();
  }
}
