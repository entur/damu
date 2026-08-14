package no.entur.damu.pubsub;

/**
 * The PubSub destinations damu uses. Topic and subscription share a name, and all three live in the
 * project pointed at by {@code spring.cloud.gcp.project-id}.
 */
public final class DamuQueues {

  /**
   * Work requests from marduk. The {@code Action} attribute says whether it is an export or an
   * aggregation.
   */
  public static final String GTFS_ROUTE_DISPATCHER_TOPIC =
    "GtfsRouteDispatcherTopic";

  /**
   * Per-codespace GTFS export status back to marduk.
   */
  public static final String DAMU_EXPORT_GTFS_STATUS_QUEUE =
    "DamuExportGtfsStatusQueue";

  /**
   * Aggregated GTFS status back to marduk.
   */
  public static final String MARDUK_AGGREGATE_GTFS_STATUS_QUEUE =
    "MardukAggregateGtfsStatusQueue";

  private DamuQueues() {}
}
