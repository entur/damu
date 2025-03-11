package no.entur.damu.routes.aggregation;

import static no.entur.damu.Constants.ORIGINAL_GTFS_FILES_SUB_FOLDER;
import static org.apache.camel.Exchange.FILE_PARENT;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import no.entur.damu.gtfs.merger.GtfsExport;
import no.entur.damu.gtfs.merger.GtfsFileUtils;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;

public class GtfsExtendedAggregationProcessor implements Processor {

  @Override
  public void process(Exchange exchange) throws Exception {
    File sourceDirectory = new File(
      exchange.getIn().getHeader(FILE_PARENT, String.class) +
      ORIGINAL_GTFS_FILES_SUB_FOLDER
    );

    Collection<File> zipFiles = new ArrayList<>(
      FileUtils.listFiles(sourceDirectory, new String[] { "zip" }, false)
    );

    exchange
      .getIn()
      .setBody(
        GtfsFileUtils.mergeGtfsFilesToInputStream(
          zipFiles,
          GtfsExport.GTFS_EXTENDED,
          true
        )
      );
  }
}
