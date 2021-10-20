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

package no.entur.damu.export.serializer;

import no.entur.damu.export.exception.GtfsSerializationException;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.serialization.GtfsWriter;
import org.onebusaway.gtfs.services.GtfsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DefaultGtfsSerializer implements GtfsSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultGtfsSerializer.class);

    private static final Map<Class<?>, Collection<String>> FILTERED_FIELDS = Map.of(
            Route.class, List.of("eligibility_restricted"),
            StopTime.class, List.of("continuous_pickup", "continuous_drop_off", "start_service_area_radius", "end_service_area_radius", "departure_buffer"),
            Trip.class, List.of("drt_advance_book_min", "peak_offpeak")
    );




    /**
     * Create a zip archive containing the GTFS dataset and return an input stream pointing to it.
     *
     * @return an input stream on the GTFS zip file.
     */
    @Override
    public InputStream writeGtfs(GtfsDao gtfsDao) {
        LOGGER.info("Exporting GTFS archive");
        GtfsWriter writer = null;
        try {
            File outputFile = File.createTempFile("damu-export-gtfs-", ".zip");
            writer = new FilteredFieldsGtfsWriter(FILTERED_FIELDS);
            writer.setOutputLocation(outputFile);
            writer.run(gtfsDao);

            return createDeleteOnCloseInputStream(outputFile);

        } catch (IOException e) {
            throw new GtfsSerializationException("Error while saving the GTFS dataset", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOGGER.warn("Error while closing the GTFS writer", e);
                }
            }
        }

    }

    /**
     * Open an input stream on a temporary file with the guarantee that the file will be deleted when the stream is closed.
     *
     * @param tmpFile a temporary file.
     * @return an Input stream pointing to the temporary file.
     */
    private static InputStream createDeleteOnCloseInputStream(File tmpFile) {
        try {
            return Files.newInputStream(tmpFile.toPath(), StandardOpenOption.DELETE_ON_CLOSE);
        } catch (IOException e) {
            throw new GtfsSerializationException("Error while creating the input stream for the GTFS archive", e);
        }
    }
}
