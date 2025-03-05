package no.entur.damu.routes.aggregation;

import no.entur.damu.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static no.entur.damu.Constants.STATUS_HEADER;

public class GtfsAggregationStatusProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(GtfsAggregationStatusProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Map<String, String> existingAttributes = exchange.getIn().getHeader("CamelGooglePubsubAttributes", Map.class);
        Map<String, String> nextAttributes = new HashMap<>(existingAttributes);
        String headerValue = exchange.getIn().getHeader(STATUS_HEADER, String.class);
        nextAttributes.put(STATUS_HEADER, headerValue);
        log.info(correlation() + "Notifying marduk of aggregation status " + headerValue);
        exchange.getIn().setHeader("CamelGooglePubsubAttributes", nextAttributes);
    }

    protected String correlation() {
        return (
                "[codespace=${header." +
                        Constants.DATASET_REFERENTIAL +
                        "} correlationId=${header." +
                        Constants.CORRELATION_ID +
                        "}] "
        );
    }
}
