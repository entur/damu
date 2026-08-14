package no.entur.damu.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import no.entur.damu.exception.DamuException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The GTFS file names arrive in the message body, so they decide both which blob is fetched and where
 * it is written.
 */
class GtfsFileNameGuardTest {

  private static final Path DIRECTORY = Path.of(
    "/tmp/damu/EXPORT_GTFS_MERGED/1/original-gtfs-files"
  );

  @Test
  void plainFileNameResolvesInsideTheDirectory() {
    assertEquals(
      DIRECTORY.resolve("rb_avi-aggregated-gtfs.zip"),
      GtfsAggregationService.destinationWithin(
        DIRECTORY,
        "rb_avi-aggregated-gtfs.zip"
      )
    );
  }

  @ParameterizedTest
  @ValueSource(
    strings = {
      "../escaped.zip",
      "../../../etc/passwd",
      "/etc/passwd",
      "nested/escaped.zip",
      "..",
    }
  )
  void nameThatEscapesTheDirectoryIsRefused(String fileName) {
    assertThrows(
      DamuException.class,
      () -> GtfsAggregationService.destinationWithin(DIRECTORY, fileName)
    );
  }
}
