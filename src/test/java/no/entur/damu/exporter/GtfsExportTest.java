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

package no.entur.damu.exporter;

import no.entur.damu.export.GtfsExport;
import no.entur.damu.export.stop.FileStopAreaRepository;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

class GtfsExportTest {

    @Test
    @Disabled
    void testExport() throws IOException {

        FileStopAreaRepository fileStopAreaRepository = new FileStopAreaRepository();

        //fileStopAreaRepository.loadStopAreas(getClass().getResourceAsStream("/RailStations_latest.zip"));
        //GtfsExport gtfsExport = new GtfsExport("FLB", getClass().getResourceAsStream("/rb_flb-aggregated-netex.zip"), fileStopAreaRepository);
        //GtfsExport gtfsExport = new GtfsExport("NSB", getClass().getResourceAsStream("/rb_nsb-aggregated-netex.zip"), fileStopAreaRepository);

        InputStream exportedGtfs = gtfsExport.exportGtfs();

        java.nio.file.Files.copy(
                exportedGtfs,
                Path.of("export-gtfs.zip"),
                StandardCopyOption.REPLACE_EXISTING);

        IOUtils.closeQuietly(exportedGtfs);
    }


}
