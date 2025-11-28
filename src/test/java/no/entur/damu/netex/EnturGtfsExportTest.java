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

package no.entur.damu.netex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.entur.netex.gtfs.export.GtfsExporter;
import org.entur.netex.gtfs.export.stop.DefaultStopAreaRepositoryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.zip.ZipUtil;

/**
 * Coarse-grained integration test that verifies that the GTFS file is generated.
 */
class EnturGtfsExportTest {

  @Test
  void testEnturExport() throws IOException {
    DefaultStopAreaRepositoryFactory factory =
      new DefaultStopAreaRepositoryFactory();
    factory.refreshStopAreaRepository(
      getClass().getResourceAsStream("/RailStations_latest.zip")
    );

    InputStream netexTimetableDataset = getClass()
      .getResourceAsStream("/rb_flb-aggregated-netex.zip");

    String codespace = "FLB";
    GtfsExporter gtfsExport = new EnturGtfsExporter(
      codespace,
      factory.getStopAreaRepository()
    );

    InputStream exportedGtfs = gtfsExport.convertTimetablesToGtfs(
      netexTimetableDataset
    );

    File gtfsFile = new File("export-gtfs.zip");
    java.nio.file.Files.copy(
      exportedGtfs,
      gtfsFile.toPath(),
      StandardCopyOption.REPLACE_EXISTING
    );

    checkAgency(gtfsFile, codespace);
    checkTrip(gtfsFile);

    IOUtils.closeQuietly(exportedGtfs);
  }

  private void checkAgency(File gtfsFile, String codespace) throws IOException {
    Iterable<CSVRecord> csvRecords = getCsvRecords(gtfsFile, "agency.txt");
    Assertions.assertTrue(csvRecords.iterator().hasNext());
    CSVRecord csvRecord = csvRecords.iterator().next();
    Assertions.assertNotNull(csvRecord.get("agency_id"));
    Assertions.assertTrue(
      csvRecord.get("agency_id").startsWith(codespace + ':' + "Authority")
    );
    Assertions.assertFalse(csvRecords.iterator().hasNext());
  }

  private void checkTrip(File gtfsFile) throws IOException {
    Iterable<CSVRecord> csvRecords = getCsvRecords(gtfsFile, "trips.txt");
    Assertions.assertTrue(csvRecords.iterator().hasNext());
    CSVRecord csvRecord = csvRecords.iterator().next();
    Assertions.assertNotNull(csvRecord.get("trip_id"));
  }

  private Iterable<CSVRecord> getCsvRecords(File gtfsFile, String entryName)
    throws IOException {
    Assertions.assertTrue(ZipUtil.containsEntry(gtfsFile, entryName));

    byte[] zipEntry = ZipUtil.unpackEntry(
      gtfsFile,
      entryName,
      StandardCharsets.UTF_8
    );
    CSVFormat csvFormat = CSVFormat.DEFAULT
      .builder()
      .setHeader()
      .setSkipHeaderRecord(true)
      .get();
    return csvFormat.parse(
      new InputStreamReader(new ByteArrayInputStream(zipEntry))
    );
  }
}
