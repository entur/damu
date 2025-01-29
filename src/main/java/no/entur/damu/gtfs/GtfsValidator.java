/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *  *
 *
 */

package no.entur.damu.gtfs;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.mobilitydata.gtfsvalidator.runner.ApplicationType;
import org.mobilitydata.gtfsvalidator.runner.ValidationRunner;
import org.mobilitydata.gtfsvalidator.runner.ValidationRunnerConfig;
import org.mobilitydata.gtfsvalidator.util.VersionResolver;

public class GtfsValidator {

  public static InputStream validate(
    String datasetReferential,
    InputStream zippedGtfsContent
  ) throws IOException {
    if (
      datasetReferential == null ||
      zippedGtfsContent == null ||
      datasetReferential.isEmpty() ||
      zippedGtfsContent.available() == 0
    ) {
      throw new IllegalArgumentException(
        "datasetReferential and zippedGtfsContent must be set"
      );
    }

    Path tempGtfsFile = Files.createTempFile(
      "gtfs-" + datasetReferential,
      ".zip"
    );
    Files.copy(
      zippedGtfsContent,
      tempGtfsFile,
      StandardCopyOption.REPLACE_EXISTING
    );

    Path gtfsValidationReportsDirectory = Files.createTempDirectory(
      "gtfs-validation-output-" + datasetReferential
    );

    ValidationRunnerConfig.Builder builder = ValidationRunnerConfig.builder();
    builder.setGtfsSource(tempGtfsFile.toUri());
    builder.setOutputDirectory(gtfsValidationReportsDirectory);
    ValidationRunner runner = new ValidationRunner(
      new VersionResolver(ApplicationType.WEB)
    );
    runner.run(builder.build());

    InputStream validationReports = zipValidationReports(
      gtfsValidationReportsDirectory
    );
    cleanup(tempGtfsFile, gtfsValidationReportsDirectory);

    return validationReports;
  }

  private static InputStream zipValidationReports(
    Path gtfsValidationReportsDirectory
  ) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      Files.walkFileTree(
        gtfsValidationReportsDirectory,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(
            Path file,
            BasicFileAttributes attrs
          ) throws IOException {
            zos.putNextEntry(
              new ZipEntry(
                gtfsValidationReportsDirectory.relativize(file).toString()
              )
            );
            Files.copy(file, zos);
            zos.closeEntry();
            return FileVisitResult.CONTINUE;
          }
        }
      );
    }
    return new ByteArrayInputStream(baos.toByteArray());
  }

  private static void cleanup(
    Path gtfsTempFilePath,
    Path gtfsValidationReportsDirectory
  ) throws IOException {
    Files.deleteIfExists(gtfsTempFilePath);
    Files.walkFileTree(
      gtfsValidationReportsDirectory,
      new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
          throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
          throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      }
    );
  }
}
