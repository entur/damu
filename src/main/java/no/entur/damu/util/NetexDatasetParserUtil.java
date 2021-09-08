package no.entur.damu.util;

import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class NetexDatasetParserUtil {

    private NetexDatasetParserUtil() {
    }

    public static NetexEntitiesIndex parse(NetexParser parser, ZipInputStream zipInputStream, NetexEntitiesIndex index) throws IOException {
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            byte[] allBytes = zipInputStream.readAllBytes();
            parser.parse(new ByteArrayInputStream(allBytes), index);
            zipEntry = zipInputStream.getNextEntry();
        }
        return index;
    }
}
