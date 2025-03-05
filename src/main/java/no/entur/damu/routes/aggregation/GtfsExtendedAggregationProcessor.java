package no.entur.damu.routes.aggregation;

import no.entur.damu.gtfs.merger.GtfsExport;
import no.entur.damu.gtfs.merger.GtfsFileUtils;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.io.File;

import static no.entur.damu.Constants.ORIGINAL_GTFS_FILES_SUB_FOLDER;
import static org.apache.camel.Exchange.FILE_PARENT;

public class GtfsExtendedAggregationProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        File sourceDirectory = new File(exchange.getIn().getHeader(FILE_PARENT, String.class) + ORIGINAL_GTFS_FILES_SUB_FOLDER);
        exchange
            .getIn()
            .setBody(GtfsFileUtils.mergeGtfsFilesInDirectory(sourceDirectory, GtfsExport.GTFS_EXTENDED, true));
    }
}
