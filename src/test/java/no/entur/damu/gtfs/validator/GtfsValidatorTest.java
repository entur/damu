package no.entur.damu.gtfs.validator;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

class GtfsValidatorTest {

  @Test
  void testValidateWithValidGtfsFile() throws IOException {
    try (
      InputStream gtfsInputStream = getClass()
        .getResourceAsStream("/rb_avi-aggregated-gtfs.zip");
      InputStream validationResult = GtfsValidator.validate(
        "rb_avi",
        gtfsInputStream
      )
    ) {
      assertNotNull(validationResult, "Validation result should not be null");

      // Check the contents of the validation reports zip
      try (ZipInputStream zis = new ZipInputStream(validationResult)) {
        ZipEntry entry;
        boolean foundReportFiles = false;

        while ((entry = zis.getNextEntry()) != null) {
          String entryName = entry.getName();

          assertTrue(
            entryName.equals("report.html") ||
            entryName.equals("report.json") ||
            entryName.equals("system_errors.json"),
            "Unexpected file in validation reports: " + entryName
          );

          foundReportFiles = true;
          zis.closeEntry();
        }

        assertTrue(
          foundReportFiles,
          "Validation reports zip should contain expected files"
        );
      }
    }
  }

  @Test
  void testValidateWithEmptyInputStream() {
    InputStream emptyInputStream = new ByteArrayInputStream(new byte[0]);

    assertThrows(
      IllegalArgumentException.class,
      () -> GtfsValidator.validate("empty-referential", emptyInputStream),
      "Validation should throw an exception for empty input"
    );
  }

  @Test
  void testValidateWithNullInputStream() {
    assertThrows(
      IllegalArgumentException.class,
      () -> GtfsValidator.validate("null-referential", null),
      "Validation should throw an exception for null input"
    );
  }

  @Test
  void testValidateWithNullReferential() {
    InputStream gtfsInputStream = getClass()
      .getResourceAsStream("/rb_avi-aggregated-gtfs.zip");
    assertThrows(
      IllegalArgumentException.class,
      () -> GtfsValidator.validate(null, gtfsInputStream),
      "Validation should throw an exception for null input"
    );
  }
}
