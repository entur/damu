package no.entur.damu.routes;

import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

@Component
public class GtfsRouteDispatcher extends BaseRouteBuilder {

  private final String GTFS_ROUTE_DISPATCHER_HEADER = "Action";
  private final String GTFS_ROUTE_DISPATCHER_AGGREGATE_HEADER = "Aggregation";
  private final String GTFS_ROUTE_DISPATCHER_EXPORT_HEADER = "Export";

  @Override
  public void configure() throws Exception {
    super.configure();

    from(
      "google-pubsub:{{marduk.pubsub.project.id}}:GtfsRouteDispatcherTopic?synchronousPull=true"
    )
      .choice()
      .when(
        header(GTFS_ROUTE_DISPATCHER_HEADER)
          .isEqualTo(GTFS_ROUTE_DISPATCHER_EXPORT_HEADER)
      )
      .log(
        LoggingLevel.INFO,
        correlation() + " Dispatching message to GTFS export route"
      )
      .to("direct:exportGtfs")
      .when(
        header(GTFS_ROUTE_DISPATCHER_HEADER)
          .isEqualTo(GTFS_ROUTE_DISPATCHER_AGGREGATE_HEADER)
      )
      .log(
        LoggingLevel.INFO,
        correlation() + " Dispatching message to GTFS aggregation route"
      )
      .to("direct:aggregateGtfs")
      .otherwise()
      .log(
        LoggingLevel.INFO,
        correlation() + " Unknown header value ${header.ACTION}, ending route"
      )
      .routeId("GtfsRouteDispatcher");
  }
}
