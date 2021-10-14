package no.entur.damu.netex;

import no.entur.damu.export.loader.DefaultNetexDatasetLoader;
import no.entur.damu.export.repository.NetexDatasetRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Custom NeTEx dataset loader that de-duplicates entities that are defined both in flexible lines (NPlan/Uttu) and non-flexible lines (Chouette).
 */
public class EnturNetexDatasetLoader extends DefaultNetexDatasetLoader {

    @Override
    protected void parseDataset(ZipInputStream zipInputStream, NetexDatasetRepository netexDatasetRepository) throws IOException {
        List<byte[]> commonFiles = new ArrayList<>();
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            // parse the common files for non-flexible lines in the end so that they do not get overwritten by the flexible common files
            if (zipEntry.getName().endsWith("_shared_data.xml") && !zipEntry.getName().endsWith("_flexible_shared_data.xml")) {
                commonFiles.add(zipInputStream.readAllBytes());
            } else {
                byte[] allBytes = zipInputStream.readAllBytes();
                netexParser.parse(new ByteArrayInputStream(allBytes), netexDatasetRepository.getIndex());
            }
            zipEntry = zipInputStream.getNextEntry();
        }
        commonFiles.forEach(commonFile -> netexParser.parse(new ByteArrayInputStream(commonFile), netexDatasetRepository.getIndex()));
    }
}

