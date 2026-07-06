/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package no.entur.damu.config;

import org.apache.camel.Exchange;
import org.apache.camel.component.google.pubsub.GooglePubsubComponent;
import org.apache.camel.component.google.pubsub.GooglePubsubHeaderFilterStrategy;
import org.apache.camel.spi.ComponentCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Restricts outbound Google PubSub message attributes to the explicit attribute map curated by
 * {@code BaseRouteBuilder}.
 *
 * <p>Since Camel 4.x the PubSub producer copies every non-filtered Camel header into the published
 * message attributes (in addition to the explicit {@code GooglePubsubConstants.ATTRIBUTES} map).
 * That would leak internal headers such as {@code RutebankenFileHandle} and
 * {@code TIMETABLE_DATASET_FILE} to downstream consumers. This strategy filters out all Camel
 * headers on the way out, so only the whitelisted attribute map is published. Inbound filtering
 * keeps the component default behaviour.
 */
@Configuration
public class GooglePubsubHeaderFilterConfig {

  @Bean
  ComponentCustomizer googlePubsubHeaderFilterCustomizer() {
    return ComponentCustomizer
      .builder(GooglePubsubComponent.class)
      .build(component ->
        component.setHeaderFilterStrategy(
          new OutboundFilteringHeaderFilterStrategy()
        )
      );
  }

  /**
   * Filters out all Camel headers when producing to PubSub. Outbound attributes are set explicitly
   * through {@code GooglePubsubConstants.ATTRIBUTES}, so no header should be mapped automatically.
   */
  static class OutboundFilteringHeaderFilterStrategy
    extends GooglePubsubHeaderFilterStrategy {

    @Override
    public boolean applyFilterToCamelHeaders(
      String headerName,
      Object headerValue,
      Exchange exchange
    ) {
      return true;
    }
  }
}
