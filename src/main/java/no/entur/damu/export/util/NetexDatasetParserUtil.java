package no.entur.damu.export.util;

import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class for parsing NeTEx archives
 */
public final class NetexDatasetParserUtil {

    private NetexDatasetParserUtil() {
    }

    /**
     * Parse a zip file containing a NeTEx archive.
     * The common files for non-flexible lines have precedence over those referring to flexible lines.
     * @param parser
     * @param zipInputStream
     * @param index
     * @return
     * @throws IOException
     */
    public static NetexEntitiesIndex parse(NetexParser parser, ZipInputStream zipInputStream, NetexEntitiesIndex index) throws IOException {
        List<byte[]> commonFiles = new ArrayList<>();
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            // parse the common files for non-flexible lines in the end so that they do not get overwritten by the flexible common files
            if (zipEntry.getName().endsWith("_shared_data.xml") && !zipEntry.getName().endsWith("_flexible_shared_data.xml")) {
                commonFiles.add(zipInputStream.readAllBytes());
            } else {
                byte[] allBytes = zipInputStream.readAllBytes();
                parser.parse(new ByteArrayInputStream(allBytes), index);
            }
            zipEntry = zipInputStream.getNextEntry();
        }
        commonFiles.forEach(commonFile -> parser.parse(new ByteArrayInputStream(commonFile), index));
        return index;
    }
}
