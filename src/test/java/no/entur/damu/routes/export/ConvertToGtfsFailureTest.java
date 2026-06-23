package no.entur.damu.routes.export;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import org.entur.netex.gtfs.export.GtfsExporter;
import org.entur.netex.gtfs.export.exception.GtfsExportException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the conversion-failure handling in {@link GtfsExportQueueRouteBuilder}.
 *
 * <p>A failure inside the converter (e.g. the {@code IndexOutOfBoundsException} thrown for
 * NeTEx journey patterns whose stop-point {@code order} values are not a dense {@code 1..N}
 * sequence) must surface as a {@link GtfsExportException} so that the route's
 * {@code onException(GtfsExportException.class)} handler marks the export failed and the
 * Pub/Sub message is acknowledged, instead of being redelivered until retention expires.
 */
class ConvertToGtfsFailureTest {

  @Test
  void nonGtfsExportExceptionIsWrappedAsGtfsExportException() {
    GtfsExporter exporter = mock(GtfsExporter.class);
    IndexOutOfBoundsException cause = new IndexOutOfBoundsException(
      "Index 5 out of bounds for length 5"
    );
    when(exporter.convertTimetablesToGtfs(any())).thenThrow(cause);
    InputStream timetableDataset = InputStream.nullInputStream();

    GtfsExportException thrown = assertThrows(
      GtfsExportException.class,
      () ->
        GtfsExportQueueRouteBuilder.convertToGtfs(
          exporter,
          timetableDataset,
          "FLT"
        )
    );

    assertInstanceOf(IndexOutOfBoundsException.class, thrown.getCause());
  }

  @Test
  void gtfsExportExceptionIsNotDoubleWrapped() {
    GtfsExporter exporter = mock(GtfsExporter.class);
    GtfsExportException original = new GtfsExportException("boom");
    when(exporter.convertTimetablesToGtfs(any())).thenThrow(original);
    InputStream timetableDataset = InputStream.nullInputStream();

    GtfsExportException thrown = assertThrows(
      GtfsExportException.class,
      () ->
        GtfsExportQueueRouteBuilder.convertToGtfs(
          exporter,
          timetableDataset,
          "FLT"
        )
    );

    assertSame(original, thrown);
  }
}
