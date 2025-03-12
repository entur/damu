package no.entur.damu.routes;

import static no.entur.damu.Constants.*;

import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

@Component
public class GtfsRouteDispatcher extends BaseRouteBuilder {

  @Override
  public void configure() throws Exception {
    super.configure();

    from(
      "google-pubsub:{{marduk.pubsub.project.id}}:GtfsRouteDispatcherTopic?synchronousPull=true"
    )
      .choice()
      .when(
        header(GTFS_ROUTE_DISPATCHER_HEADER_NAME)
          .isEqualTo(GTFS_ROUTE_DISPATCHER_EXPORT_HEADER_VALUE)
      )
      .log(
        LoggingLevel.INFO,
        correlation() + " Dispatching message to GTFS export route"
      )
      .to("direct:exportGtfs")
      .when(
        header(GTFS_ROUTE_DISPATCHER_HEADER_NAME)
          .isEqualTo(GTFS_ROUTE_DISPATCHER_AGGREGATION_HEADER_VALUE)
      )
      .log(
        LoggingLevel.INFO,
        correlation() + " Dispatching message to GTFS aggregation route"
      )
      .to("direct:aggregateGtfs")
      .otherwise()
      .log(
        LoggingLevel.INFO,
        correlation() + " Unknown header value ${header.Action}, ending route"
      )
      .routeId("GtfsRouteDispatcher");
  }
}
