/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *  *
 *
 */

package no.entur.damu.pubsub;

import java.util.Set;
import no.entur.damu.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.component.google.pubsub.GooglePubsubHeaderFilterStrategy;
import org.springframework.stereotype.Component;

/**
 * Custom header filter strategy for Google PubSub.
 * <p>
 * When sending messages to PubSub (OUT direction), only whitelisted headers are allowed.
 * When receiving messages from PubSub (IN direction), all headers except internal Camel
 * PubSub headers are allowed.
 */
@Component("damuPubSubHeaderFilterStrategy")
public class DamuPubSubHeaderFilterStrategy
  extends GooglePubsubHeaderFilterStrategy {

  private static final Set<String> WHITELISTED_HEADERS = Set.of(
    Constants.CORRELATION_ID,
    Constants.DATASET_REFERENTIAL,
    Constants.PROVIDER_ID,
    Constants.ORIGINAL_PROVIDER_ID,
    Constants.STATUS_HEADER,
    Constants.GTFS_ROUTE_DISPATCHER_HEADER_NAME,
    Exchange.BREADCRUMB_ID
  );

  public DamuPubSubHeaderFilterStrategy() {
    super();
  }

  /**
   * Filter headers when sending to PubSub (OUT direction).
   * Only whitelisted headers are allowed through (returns false for whitelisted headers).
   *
   * @param headerName  the header name
   * @param headerValue the header value
   * @param exchange    the exchange
   * @return true if the header should be filtered out (excluded), false if it should be included
   */
  @Override
  public boolean applyFilterToCamelHeaders(
    String headerName,
    Object headerValue,
    Exchange exchange
  ) {
    // First apply the parent filter (filters internal Google/Camel headers)
    if (super.applyFilterToCamelHeaders(headerName, headerValue, exchange)) {
      return true;
    }
    // Only allow whitelisted headers through (filter out non-whitelisted)
    return !WHITELISTED_HEADERS.contains(headerName);
  }

  /**
   * Filter headers when receiving from PubSub (IN direction).
   * Internal CamelGooglePubsub headers are filtered out.
   *
   * @param headerName  the header name
   * @param headerValue the header value
   * @param exchange    the exchange
   * @return true if the header should be filtered out (excluded), false if it should be included
   */
  @Override
  public boolean applyFilterToExternalHeaders(
    String headerName,
    Object headerValue,
    Exchange exchange
  ) {
    // First apply the parent filter
    if (super.applyFilterToExternalHeaders(headerName, headerValue, exchange)) {
      return true;
    }
    // Filter out internal CamelGooglePubsub headers
    return headerName.startsWith("CamelGooglePubsub");
  }
}
