package no.entur.damu.gtfs.merger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BasicRouteTypeCodeTest {

  @Test
  void convertRouteTypeThrowsWhenUnknownRouteTypeCode() {
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> {
        BasicRouteTypeCode.convertRouteType(1100);
      }
    );
  }

  @Test
  void convertTaxiRouteTypeToBus() {
    Integer routeType = BasicRouteTypeCode.convertRouteType(1501);
    Assertions.assertEquals(BasicRouteTypeCode.BUS.getCode(), routeType);
  }
}
