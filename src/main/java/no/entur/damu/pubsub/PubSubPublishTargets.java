package no.entur.damu.pubsub;

import org.entur.pubsub.base.EnturGooglePubSubAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Creates the destinations damu only publishes to.
 *
 * <p>Camel's autocreate notifier walked every endpoint in the context, so it created the status topics
 * as a side effect of them being producers. The consumer base class only creates what it subscribes to,
 * which covers GtfsRouteDispatcherTopic and nothing else, and the first status notification against an
 * uninitialised emulator would answer NOT_FOUND.
 *
 * <p>A no-op wherever {@code entur.pubsub.subscriber.autocreate} is false, which is every deployed
 * environment. Gated on the consumer flag because that is the one saying this instance talks to PubSub:
 * the pipeline tests point at an unreachable emulator, where an admin call would stall the context.
 */
@Component
@ConditionalOnProperty(
  value = "damu.pubsub.consumers.enabled",
  matchIfMissing = true
)
class PubSubPublishTargets {

  private final EnturGooglePubSubAdmin enturGooglePubSubAdmin;

  PubSubPublishTargets(EnturGooglePubSubAdmin enturGooglePubSubAdmin) {
    this.enturGooglePubSubAdmin = enturGooglePubSubAdmin;
  }

  @EventListener
  void handleContextRefreshed(ContextRefreshedEvent contextRefreshedEvent) {
    enturGooglePubSubAdmin.createSubscriptionIfMissing(
      DamuQueues.DAMU_EXPORT_GTFS_STATUS_QUEUE
    );
    enturGooglePubSubAdmin.createSubscriptionIfMissing(
      DamuQueues.MARDUK_AGGREGATE_GTFS_STATUS_QUEUE
    );
  }
}
