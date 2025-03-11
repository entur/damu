package no.entur.damu.routes.aggregation;

import static no.entur.damu.Constants.ORIGINAL_GTFS_FILES_SUB_FOLDER;
import static org.apache.camel.Exchange.FILE_PARENT;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import no.entur.damu.Constants;
import no.entur.damu.gtfs.merger.GtfsExport;
import no.entur.damu.gtfs.merger.GtfsFileUtils;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;

public class GtfsBasicAggregationProcessor implements Processor {

  private List<String> excludedGtfsFiles;

  public GtfsBasicAggregationProcessor(Exchange exchange) {
    String excluded = exchange
      .getContext()
      .resolvePropertyPlaceholders(
        "{{damu.gtfs.aggregation.excludedFiles:rb_avi-aggregated-gtfs.zip}}"
      );
    this.excludedGtfsFiles = Arrays.asList(excluded.split(","));
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    File sourceDirectory = new File(
      exchange.getIn().getHeader(FILE_PARENT, String.class) +
      ORIGINAL_GTFS_FILES_SUB_FOLDER
    );

    if (sourceDirectory == null || !sourceDirectory.isDirectory()) {
      throw new RuntimeException(sourceDirectory + " is not a directory");
    }

    Collection<File> zipFiles = FileUtils
      .listFiles(sourceDirectory, new String[] { "zip" }, false)
      .stream()
      .filter(file -> !excludedGtfsFiles.contains(file.getName()))
      .collect(Collectors.toList());

    boolean includeShapes = exchange
      .getIn()
      .getHeader(Constants.INCLUDE_SHAPES, Boolean.class);

    exchange
      .getIn()
      .setBody(
        GtfsFileUtils.mergeGtfsFilesToInputStream(
          zipFiles,
          GtfsExport.GTFS_BASIC,
          includeShapes
        )
      );
  }
}
