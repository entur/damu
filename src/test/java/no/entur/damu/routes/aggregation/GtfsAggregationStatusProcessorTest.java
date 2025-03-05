package no.entur.damu.routes.aggregation;

import static no.entur.damu.Constants.*;
import static no.entur.damu.Constants.STATUS_MERGE_STARTED;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GtfsAggregationStatusProcessorTest extends CamelTestSupport {

  @Override
  protected RoutesBuilder createRouteBuilder() {
    return new RouteBuilder() {
      @Override
      public void configure() {
        from("direct:testEndpoint")
          .process(new GtfsAggregationStatusProcessor())
          .to("mock:result");
      }
    };
  }

  @BeforeEach
  public void setup() {
    template.setDefaultEndpointUri("direct:testEndpoint");
  }

  @Test
  public void testProcessorStatusMessageOk() throws Exception {
    MockEndpoint mockResult = getMockEndpoint("mock:result");

    Map<String, Object> expectedCamelHeaderValue = new HashMap<>();
    expectedCamelHeaderValue.put(STATUS_HEADER, STATUS_MERGE_OK);
    mockResult.expectedHeaderReceived(
      "CamelGooglePubsubAttributes",
      expectedCamelHeaderValue
    );

    Map<String, Object> headers = new HashMap<>();
    headers.put(STATUS_HEADER, STATUS_MERGE_OK);
    headers.put("CamelGooglePubsubAttributes", new HashMap<>());
    template.sendBodyAndHeaders("", headers);
    mockResult.assertIsSatisfied();
  }

  @Test
  public void testProcessorStatusMessageStarted() throws Exception {
    MockEndpoint mockResult = getMockEndpoint("mock:result");

    Map<String, Object> expectedCamelHeaderValue = new HashMap<>();
    expectedCamelHeaderValue.put(STATUS_HEADER, STATUS_MERGE_STARTED);
    mockResult.expectedHeaderReceived(
      "CamelGooglePubsubAttributes",
      expectedCamelHeaderValue
    );

    Map<String, Object> headers = new HashMap<>();
    headers.put(STATUS_HEADER, STATUS_MERGE_STARTED);
    headers.put("CamelGooglePubsubAttributes", new HashMap<>());
    template.sendBodyAndHeaders("", headers);
    mockResult.assertIsSatisfied();
  }

  @Test
  public void testProcessorStatusMessageFailed() throws Exception {
    MockEndpoint mockResult = getMockEndpoint("mock:result");

    Map<String, Object> expectedCamelHeaderValue = new HashMap<>();
    expectedCamelHeaderValue.put(STATUS_HEADER, STATUS_MERGE_FAILED);
    mockResult.expectedHeaderReceived(
      "CamelGooglePubsubAttributes",
      expectedCamelHeaderValue
    );

    Map<String, Object> headers = new HashMap<>();
    headers.put(STATUS_HEADER, STATUS_MERGE_FAILED);
    headers.put("CamelGooglePubsubAttributes", new HashMap<>());
    template.sendBodyAndHeaders("", headers);
    mockResult.assertIsSatisfied();
  }
}
