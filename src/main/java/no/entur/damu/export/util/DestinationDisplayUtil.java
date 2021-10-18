package no.entur.damu.export.util;

import no.entur.damu.export.repository.NetexDatasetRepository;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for Destination Displays.
 */
public final class DestinationDisplayUtil {

    private DestinationDisplayUtil() {
    }

    public static DestinationDisplay getInitialDestinationDisplay(JourneyPattern journeyPattern, NetexDatasetRepository netexDatasetRepository) {
        StopPointInJourneyPattern firstStopPointInJourneyPattern = (StopPointInJourneyPattern) journeyPattern.getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                .get(0);
        return netexDatasetRepository.getDestinationDisplayById(firstStopPointInJourneyPattern.getDestinationDisplayRef().getRef());
    }

    /**
     * Compute a front text (GTFS head sign) from a destination display that may contain a list of vias (intermediate head sign)
     *
     * @param destinationDisplay     the NeTEx destination display
     * @param netexDatasetRepository the netex dataset repository
     * @return a front text that concatenates the destination display and its optional vias.
     */
    public static String getFrontTextWithComputedVias(DestinationDisplay destinationDisplay, NetexDatasetRepository netexDatasetRepository) {

        if (destinationDisplay == null) {
            return null;
        }

        List<DestinationDisplay> vias;
        if (destinationDisplay.getVias() != null) {
            vias = destinationDisplay.getVias()
                    .getVia()
                    .stream()
                    .map(via -> via.getDestinationDisplayRef().getRef())
                    .map(netexDatasetRepository::getDestinationDisplayById)
                    .collect(Collectors.toList());
        } else {
            vias = Collections.emptyList();
        }
        String frontText = destinationDisplay.getFrontText().getValue();
        if (!vias.isEmpty() && frontText != null) {
            StringBuilder b = new StringBuilder();
            b.append(frontText);
            b.append(" via ");
            List<String> viaFrontTexts = new ArrayList<>();
            for (DestinationDisplay via : vias) {
                if (via.getFrontText() != null) {
                    viaFrontTexts.add(via.getFrontText().getValue());
                }
            }
            b.append(String.join("/", viaFrontTexts));
            return b.toString();
        } else {
            return frontText;
        }
    }
}
