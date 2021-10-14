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
