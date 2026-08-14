package no.entur.damu.pubsub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PubSubAttributesTest {

  @Test
  void echoDropsTheClientLibraryAttributes() {
    Map<String, String> echoed = PubSubAttributes.echo(
      Map.of(
        "RutebankenCorrelationId",
        "corr-1",
        PubSubAttributes.DELIVERY_ATTEMPT,
        "3"
      )
    );

    assertEquals("corr-1", echoed.get("RutebankenCorrelationId"));
    assertFalse(
      echoed.containsKey(PubSubAttributes.DELIVERY_ATTEMPT),
      "Republishing a googclient_ attribute puts something on the wire marduk never sent"
    );
  }

  @Test
  void deliveryAttemptIsZeroWhenTheSubscriptionHasNoDeadLetterPolicy() {
    assertEquals(0, PubSubAttributes.deliveryAttempt(Map.of()));
  }

  @Test
  void deliveryAttemptIsReadFromTheClientLibraryAttribute() {
    assertEquals(
      4,
      PubSubAttributes.deliveryAttempt(
        Map.of(PubSubAttributes.DELIVERY_ATTEMPT, "4")
      )
    );
  }
}
