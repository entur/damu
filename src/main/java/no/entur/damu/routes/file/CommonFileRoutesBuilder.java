package no.entur.damu.routes.file;

import no.entur.damu.routes.BaseRouteBuilder;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

@Component
public class CommonFileRoutesBuilder extends BaseRouteBuilder {

    @Override
    public void configure() throws Exception {
        super.configure();
        from("direct:cleanUpLocalDirectory")
            .process(e -> deleteDirectoryRecursively(e.getIn().getHeader(Exchange.FILE_PARENT, String.class)))
            .routeId("cleanup-local-dir");
    }
}