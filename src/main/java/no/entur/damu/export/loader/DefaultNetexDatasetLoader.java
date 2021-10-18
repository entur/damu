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

package no.entur.damu.export.loader;

import no.entur.damu.export.exception.NetexParsingException;
import no.entur.damu.export.repository.NetexDatasetRepository;
import org.entur.netex.NetexParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DefaultNetexDatasetLoader implements NetexDatasetLoader {

    protected final NetexParser netexParser;

    public DefaultNetexDatasetLoader() {
        this.netexParser = new NetexParser();
    }

    @Override
    public void load(InputStream timetableDataset, NetexDatasetRepository netexDatasetRepository) {
        try (ZipInputStream zipInputStream = new ZipInputStream(timetableDataset)) {
            parseDataset(zipInputStream, netexDatasetRepository);
        } catch (IOException e) {
            throw new NetexParsingException("Error while parsing the NeTEx timetable dataset", e);
        }
    }


    /**
     * Parse a zip file containing a NeTEx archive.
      *
     * @param zipInputStream
     * @param netexDatasetRepository
     * @throws IOException
     */
    protected void parseDataset(ZipInputStream zipInputStream, NetexDatasetRepository netexDatasetRepository) throws IOException {
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            byte[] allBytes = zipInputStream.readAllBytes();
            netexParser.parse(new ByteArrayInputStream(allBytes), netexDatasetRepository.getIndex());
            zipEntry = zipInputStream.getNextEntry();
        }

    }
}
