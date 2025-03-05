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

package no.entur.damu.routes.file;

import java.io.*;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import no.entur.damu.exception.FileValidationException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.zeroturnaround.zip.ZipUtil;

/**
 * Utility class for creating and reading zip files.
 */
public class ZipFileUtils {

  private ZipFileUtils() {}

  /**
   * List the entries in the zip file.
   * The byte array is first saved to disk to avoid using a ZipInputStream that would parse the whole stream to
   * find entries.
   * @param data a byte array containing a zip archive.
   * @return the set of entries in the zip archive.
   * @throws IOException
   * @throws RuntimeException if an entry is not UTF8-encoded.
   */
  public static Set<ZipEntry> listFilesInZip(byte[] data) throws IOException {
    File tmpFile = createTempFile(data, "marduk-list-files-in-zip-", ".zip");
    Set<ZipEntry> fileList = listFilesInZip(tmpFile);
    Files.delete(tmpFile.toPath());
    return fileList;
  }

  public static File createTempFile(byte[] data, String prefix, String suffix)
    throws IOException {
    File inputFile = File.createTempFile(prefix, suffix);
    try (FileOutputStream fos = new FileOutputStream(inputFile)) {
      fos.write(data);
    }
    return inputFile;
  }

  /**
   * List the entries in the zip file.
   * The byte array is first saved to disk to avoid using a ZipInputStream that would parse the whole stream to
   * find entries.
   * @param file the zip archive.
   * @return the set of entries in the zip archive.
   * @throws IOException
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
