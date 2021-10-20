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

package no.entur.damu.netex;

import org.entur.netex.gtfs.export.GtfsExporter;
import org.entur.netex.gtfs.export.stop.DefaultStopAreaRepository;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

class EnturGtfsExportTest {

    @Test
    @Disabled
    void testEnturExport() throws IOException {

        DefaultStopAreaRepository defaultStopAreaRepository = new DefaultStopAreaRepository();
        defaultStopAreaRepository.loadStopAreas(getClass().getResourceAsStream("/RailStations_latest.zip"));

        InputStream netexTimetableDataset = getClass().getResourceAsStream("/rb_flb-aggregated-netex.zip");

        GtfsExporter gtfsExport = new EnturGtfsExporter("FLB", defaultStopAreaRepository);


        InputStream exportedGtfs = gtfsExport.convertNetexToGtfs(netexTimetableDataset);

        java.nio.file.Files.copy(
                exportedGtfs,
                Path.of("export-gtfs-entur.zip"),
                StandardCopyOption.REPLACE_EXISTING);

        IOUtils.closeQuietly(exportedGtfs);
    }


}
