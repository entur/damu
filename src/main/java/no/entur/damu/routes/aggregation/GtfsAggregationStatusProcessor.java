package no.entur.damu.routes.aggregation;

import static no.entur.damu.Constants.STATUS_HEADER;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class GtfsAggregationStatusProcessor implements Processor {

  @Override
  public void process(Exchange exchange) throws Exception {
    Map<String, String> existingAttributes = exchange
      .getIn()
      .getHeader("CamelGooglePubsubAttributes", Map.class);
    Map<String, String> nextAttributes = new HashMap<>(existingAttributes);
    String headerValue = exchange
      .getIn()
      .getHeader(STATUS_HEADER, String.class);
    nextAttributes.put(STATUS_HEADER, headerValue);
    exchange.getIn().setHeader("CamelGooglePubsubAttributes", nextAttributes);
  }
}
