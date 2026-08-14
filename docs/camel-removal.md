# Removing Apache Camel from damu

What replaced Camel, and every behaviour difference the swap introduced. Written against
`antu/docs/camel-migration-learnings.md`; where this file and that one disagree about damu, this one wins.

## What damu does

Unchanged by this work. One PubSub subscription carries two kinds of request from marduk, and two
scheduled jobs keep the stop data current.

| Trigger | Job |
| --- | --- |
| `GtfsRouteDispatcherTopic`, `Action=Export` | download the NeTEx export for a codespace, convert it to GTFS, validate it, upload both, tell marduk |
| `GtfsRouteDispatcherTopic`, `Action=Aggregation` | download the per-provider GTFS exports named in the body, merge them into the extended and basic national datasets, upload both, tell marduk |
| cron, 01:00 and 14:00 | reload the stop area repository from `tiamat/CurrentAndFuture_latest.zip` |
| cron, 03:30 | export the stop places to `tiamat/Current_latest-gtfs.zip` |

## The mapping

| Was | Is |
| --- | --- |
| `GtfsRouteDispatcher` route, `google-pubsub:...?synchronousPull=true&ackMode=AUTO&maxDeliveryAttempts=5` | `GtfsRouteDispatcherConsumer` on `AbstractEnturGooglePubSubConsumer` |
| `direct:exportGtfs` and its sub-routes | `GtfsExportService` |
| `direct:validateGtfs` and its sub-routes | `GtfsValidationService` |
| `direct:aggregateGtfs` and its sub-routes, plus the three `Processor` classes | `GtfsAggregationService` |
| `quartz://damu/exportStopsPeriodically` | `GtfsStopExportService.exportStops`, `@Scheduled(cron)` |
| `quartz://damu/refreshStopsPeriodically` | `StopAreaRefreshService.refreshStops`, `@Scheduled(cron)` |
| `quartz://damu/refreshStopsAtStartup` (`trigger.repeatCount=0`) | `StopAreaRefreshService.refreshStopsAtStartup`, `@EventListener(ApplicationReadyEvent)` |
| `direct:getMardukBlob` / `uploadMardukBlob` / `getBlob` / `uploadBlob` / `uploadDamuBlob` | direct calls on `MardukBlobStoreService` and `DamuBlobStoreService` |
| `interceptSendToEndpoint` whitelist plus `GooglePubsubHeaderFilterConfig` | `PubSubAttributes.echo` at each publish site |
| `interceptFrom` MDC interceptor and `onCompletion` cleanup | `DamuMdc`, set by the consumer and by each scheduled job |
| `PubSubAutoCreateEventNotifier` | `PubSubPublishTargets` |
| `BaseRouteBuilder.extendAckDeadline` | nothing; see *Ack deadline* below |

`no.entur.damu.routes` is gone. `ZipFileUtils` had no caller in `src/main`, so it moved to the test
tree alongside its only user, taking `FileValidationException` with it.

## The wire contract

Everything below is matched by name from outside this repository. `WireContractTest` asserts each of
them against a hard-coded string rather than against the constant that holds it, because comparing a
value to its own constant pins nothing: renaming the value keeps both sides in step and the test green.

- topics and subscriptions: `GtfsRouteDispatcherTopic`, `DamuExportGtfsStatusQueue`,
  `MardukAggregateGtfsStatusQueue`
- request attribute `Action`, values `Export` and `Aggregation`
- attributes echoed back to marduk: `RutebankenCorrelationId`, `RutebankenProviderId`,
  `RutebankenOriginalProviderId`, `EnturDatasetReferential`
- export status, in the **message body**: `started`, `ok`, `failed`
- aggregation status, in the **`status` attribute**: `started`, `ok`, `failed`
- the aggregation request body is a comma-separated file list
- blob paths: `outbound/netex/<codespace>-aggregated-netex.zip`, `<folder>/gtfs/<codespace>-aggregated-gtfs.zip`,
  `gtfsreport.entur.org/<codespace>-gtfs-validation-reports.zip`, `outbound/gtfs/rb_norway-aggregated-gtfs.zip`,
  `outbound/gtfs/rb_norway-aggregated-gtfs-basic.zip`, `tiamat/Current_latest-gtfs.zip`
- `maxDeliveryAttempts=5`, which has to equal `dead_letter_policy.max_delivery_attempts` on the
  GtfsRouteDispatcherTopic subscription in marduk's `terraform/pubsub.tf`

Changing a `static final String` and running `mvn test` can pass against stale bytecode, because
constants are inlined into their call sites. Verify any change to these with `mvn clean verify`.

### Attribute handling

Camel put the inbound attribute map on the exchange and used it as the *base* for every outbound
publish, then merged a whitelist of headers on top. So damu echoed the whole request back, not just the
whitelisted names, and the code that looked like a whitelist was only deciding what to add. The
replacement does the same thing explicitly: `PubSubAttributes.echo` copies the request attributes, and
each publish site adds what it owns.

Two differences, both deliberate:

- **`breadcrumbId` is gone.** Camel set it (`setUseBreadcrumb(true)`) and it was in the whitelist, so it
  was published. Marduk strips `breadcrumbId` from its own outbound attributes and never reads damu's,
  so nothing consumed it.
- **`goog*` attributes are filtered.** The streaming pull client delivers the delivery attempt counter
  as the message attribute `googclient_deliveryattempt`; the synchronous pull Camel used carried it as a
  protobuf field instead, so it never reached the attribute map. `goog` is reserved by PubSub, so
  echoing one back is rejected on publish with INVALID_ARGUMENT. Camel's producer skipped the same
  prefix (`GooglePubsubConstants.RESERVED_GOOGLE_CLIENT_ATTRIBUTE_PREFIX`, read off the 4.21.0 jar: the
  value is `"goog"`, not `"googclient_"`), so this only restores what the transport change took away.

The **aggregation status body** is now always empty. Camel sent whatever the exchange was carrying,
which was the file list on `started` and an empty string afterwards. Marduk's `gtfs-aggregate-status-route`
switches on the `status` attribute and never reads the body.

## Ack deadline

Camel consumed with `synchronousPull=true`, which does not extend the ack deadline, so the routes called
`modifyAckDeadline` by hand between steps, extending by `damu.camel.pubsub.deadline.extension=600`
each time. The streaming pull client behind `AbstractEnturGooglePubSubConsumer` extends it on its own
while a message is being processed. Every `extendAckDeadline` call is gone and nothing replaces it.

The two schemes fail differently, and neither is strictly better:

- Camel extended to 600s from *each checkpoint*, with no overall cap. Any single step that took longer
  than 600s — a merge of every provider's GTFS is a candidate — let the deadline lapse and the request
  was redelivered while it was still being worked on.
- The streaming client extends continuously, but only up to `maxAckExtensionPeriod`. Read off the jar:
  `spring-cloud-gcp` holds it as a boxed `Long` and skips the setter when it is unset, so
  `Subscriber.DEFAULT_MAX_ACK_EXTENSION_PERIOD` applies, which is `Duration.ofMinutes(60)`. A request
  that takes over an hour is redelivered. Raise
  `spring.cloud.gcp.pubsub.subscriber.max-ack-extension-period` if that ever becomes the failure.

The subscription's `ack_deadline_seconds` is 600 and its `retry_policy.minimum_backoff` is 10s; neither
changed.

## Concurrency

`synchronousPull=true` with the default `maxMessagesPerPoll=1` and one consumer meant strictly one
request at a time. The streaming client defaults to 4 callback threads and 1000 leased messages, which
would let several exports or aggregations run at once in a pod sized for one. The ConfigMap pins
`spring.cloud.gcp.pubsub.subscriber.executor-threads=1` and
`flow-control.max-outstanding-element-count=1` to keep the old behaviour.

damu runs at `forceReplicas: 1`, and did under Camel too. Nothing here coordinates across pods.

## Retries and failure handling

`damu.camel.redelivery.max=0` in every deployed environment, so the `defaultErrorHandler` in
`BaseRouteBuilder` did nothing: the code default of 3 was never in effect. There is no in-process retry
to replace.

| Failure | Then | Now |
| --- | --- | --- |
| `GtfsExportException` during an export | handled, `failed` to marduk, message acked | same |
| anything else during an export | exchange fails, message nacked, PubSub redelivers | exception escapes `onMessage`, message nacked, PubSub redelivers |
| NeTEx export missing | route stopped, marduk left with only `started` | same, and pinned by a test |
| anything during an aggregation | handled, `failed` to marduk, message acked | same |
| 5th delivery of any request | nacked unprocessed, PubSub dead-letters it | same, from the `googclient_deliveryattempt` attribute |

Two things the old code did that are preserved rather than fixed, because changing either changes what
marduk records:

- a missing NeTEx export leaves the job with a `started` and no terminal status
- a failed aggregation leaves its downloads in `gtfs.export.download.directory`; only the happy path
  cleans up

## The multicast

`multicast().to("direct:validateGtfs", "direct:uploadGtfsDataset")` ran sequentially with
`stopOnException` at its default of false, so both branches ran even when the first failed, and the
failure still reached the caller: the default `UseLatestAggregationStrategy` propagates an exception
from the earlier sub-exchange onto the later one, and `MulticastProcessor.doDone` copies it back onto the
original. `GtfsExportService.validateAndUpload` reproduces exactly that: catch, upload, rethrow.

The GTFS archive is now written to a temp file that both steps read. Camel's per-route
`streamCache("true")` did the same thing, spooling to disk above the threshold, which mattered because
the exporter hands back a stream over a temp file it deletes on close and can only be read once.

## Shutdown

No drain, and the window for in-flight work got **shorter**, from 25s to 10s.

Camel bounded it with `damu.shutdown.timeout=25`, against a `terminationGracePeriodSeconds` of 30 (the
chart default). Now the bound comes from `AbstractEnturGooglePubSubConsumer`, whose `ContextClosedEvent`
listener calls `stopAsync().awaitTerminated(10, SECONDS)` per subscriber. gax will not terminate while a
callback is running, so for a job still in progress that wait always expires, and the context proceeds to
destroy beans underneath it.

That matters because `CachingPublisherFactory.shutdown()` is `@PreDestroy` and shuts every cached
`Publisher`. A job that finishes after the 10s therefore cannot publish its terminal status: marduk is
left with a `started`, the message is nacked, and the whole export runs again.
`spring.cloud.gcp.pubsub.publisher.executor-accept-tasks-after-context-close=true` narrows this but does
not close it, because it only keeps the publisher's *thread pool* accepting work; the `Publisher` itself
is still shut down with the bean. Camel was not exposed to any of this, because its google-pubsub
component built its own publishers outside the Spring lifecycle.

In practice neither 25s nor 10s finishes a real export, so both versions abandon and redeliver it. What
changed is the band of jobs that used to complete in 10-25s and now do not. Closing it properly means an
`InFlightMessages`-style `SmartLifecycle` drain like antu's, sized against the 30s grace period, which
would buy back about 15 seconds. That has not been done: it is a real gap, deliberately left open, and
the argument for filling it gets stronger if exports get faster or the grace period gets longer.

## Scheduling

Quartz trigger URIs became Spring cron expressions, which means new ConfigMap keys:

| Old key | New key | Value |
| --- | --- | --- |
| `damu.netex.stop.cache.refresh.quartz.trigger` | `damu.netex.stop.cache.refresh.cron` | `0 0 1,14 * * *` |
| (unset, code default `?cron=0+30+03+?+*+*`) | `damu.netex.stop.export.cron` | `0 30 3 * * *` |

`@Scheduled` parses its cron eagerly, so an unparseable value fails the context at startup rather than
silently never firing. The test properties set both to real values for that reason.

Quartz ran jobs on its own scheduler with `stateful=false`, so two runs of the same job could overlap.
Spring's task scheduler serialises them per thread; `spring.task.scheduling.pool.size=2` gives the two
jobs a thread each so a long refresh cannot hold up the export.

`@EnableScheduling` needs a bean named `taskScheduler`, and Spring Boot's auto-configuration does not
supply one here: it backs off when another `TaskScheduler` bean exists, and spring-cloud-gcp registers
two (`pubsubPublisherThreadPool` and `globalPubSubSubscriberThreadPoolScheduler`). Left alone,
`@Scheduled` logs an ambiguity warning on every boot and falls back to a single-threaded executor of its
own, silently ignoring `spring.task.scheduling.pool.size`. `SchedulingConfig` declares the bean Boot
would have. This was found by reading a local-k8s boot log, not by a test; antu has the same warning and
the same silently-ignored property.

The startup refresh now blocks `ApplicationReadyEvent`, which delays `ReadinessState.ACCEPTING_TRAFFIC`
until the stop areas are loaded. Liveness is already `CORRECT` by then, so this cannot restart the pod.
It does **not** close the window where a request arrives before the repository is loaded: the PubSub
consumer starts on `ContextRefreshedEvent`, earlier still. `getStopAreaRepository()` throws
`IllegalStateException` in that window, which is not a `GtfsExportException`, so the message is nacked
and redelivered. Same as under Camel, just narrower.

## Publish-only destinations

Camel's autocreate notifier walked every endpoint in the context, so it created the two status topics as
a side effect of them being producers. `AbstractEnturGooglePubSubConsumer` only creates what it
subscribes to. `PubSubPublishTargets` creates `DamuExportGtfsStatusQueue` and
`MardukAggregateGtfsStatusQueue` explicitly. It is a no-op wherever `entur.pubsub.subscriber.autocreate`
is false, which is every deployed environment and the compose stack; it exists for a fresh emulator.

`entur.pubsub.subscriber.autocreate` defaults to **true**, and the consumer base class calls
`createSubscriptionIfMissing` on `ContextRefreshedEvent`, so the ConfigMap has to set it to false or the
pods try to create terraformed topics and fail on `pubsub.topics.create`.

## Testing

One Spring context, an in-memory blob store, a recording publisher, and the services called directly.
`PubSubWiringTest` is the only test that starts an emulator, and it only checks that a request is
consumed and a status is published.

`TestApp` now repeats the filters `@SpringBootApplication` contributes. Without `TypeExcludeFilter`,
every `@TestConfiguration` under `no.entur.damu` applies to every test that boots it, so one test's
doubles silently replace another's.

The blob store is a singleton behind both repositories, so `DamuPipelineTestBase` empties it before each
test. Without that, one test's uploads satisfy another test's assertions depending on method order.

## The release

Following the rule from the antu writeup: **nothing was removed from helm or terraform**. The Camel-era
ConfigMap keys are still there, in a marked block, because helm applies the ConfigMap before the first
pod is replaced and kubelet re-syncs it into pods still running the old jar. The one that matters most is
`damu.netex.stop.cache.refresh.quartz.trigger`: dropping it would not fail the old code, it would move
the stop place cache refresh from 01:00 and 14:00 to 03:00 with nothing saying so.

Delete that block in a later release. The values it renders are literals in the ConfigMap, so there is nothing in `values.yaml` to delete with it.

Rollback: `git revert` plus a full redeploy, or `helm rollback`. An image-only rollback
(`kubectl rollout undo`) restores the pod template but not the ConfigMap, which is a separate unversioned
object referenced by name — that is survivable here only because the old keys are still present.

Nothing needs to be deployed downstream first. Damu introduces no new status value and no new attribute;
it only stops sending `breadcrumbId`, which nothing reads.
