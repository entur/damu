package no.entur.damu.gtfs.merger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BasicRouteTypeCodeTest {

    @Test
    void convertRouteType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            BasicRouteTypeCode.convertRouteType(1100);
        });
    }
}