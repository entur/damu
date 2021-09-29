package no.entur.damu.export.util;

import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.DestinationDisplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class DestinationDisplayUtil {

    private DestinationDisplayUtil() {
    }

    public static String getFrontTextWithComputedVias(DestinationDisplay destinationDisplay, NetexEntitiesIndex netexEntitiesIndex) {

        List<DestinationDisplay> vias;
        if (destinationDisplay.getVias() != null) {
            vias = destinationDisplay.getVias()
                    .getVia()
                    .stream()
                    .map(via -> via.getDestinationDisplayRef().getRef())
                    .map(netexEntitiesIndex.getDestinationDisplayIndex()::get)
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
