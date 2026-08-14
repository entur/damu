/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package no.entur.damu.gtfs.merger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.zeroturnaround.zip.ZipUtil;

/**
 * Utility class for creating and reading zip files.
 */
public class ZipFileUtils {

  private ZipFileUtils() {}

  /**
   * List the entries in the zip file.
   * @param file the zip archive.
   * @return the set of entries in the zip archive.
   * @throws RuntimeException if an entry is not UTF8-encoded.
   */
  public static Set<ZipEntry> listFilesInZip(File file) {
    try (ZipFile zipFile = new ZipFile(file)) {
      return zipFile.stream().collect(Collectors.toSet());
    } catch (IllegalArgumentException e) {
      Throwable rootCause = ExceptionUtils.getRootCause(e);
      if (rootCause instanceof MalformedInputException) {
        throw new FileValidationException(e);
      } else {
        throw new RuntimeException(e);
      }
    } catch (ZipException e) {
      if (
        "invalid CEN header (bad entry name or comment)".equals(e.getMessage())
      ) {
        throw new FileValidationException(e);
      } else {
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] extractFileFromZipFile(
    File zipFile,
    String extractFileName
  ) {
    return ZipUtil.unpackEntry(zipFile, extractFileName);
  }
}
