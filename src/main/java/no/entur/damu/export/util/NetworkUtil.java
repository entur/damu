package no.entur.damu.export.util;

import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.Network;

public final class NetworkUtil {

    private NetworkUtil() {
    }

    /**
     * Return the network referenced by the <RepresentedByGroupRef>.
     * RepresentedByGroupRef can reference a network either directly or indirectly (through a group of lines)
     *
     * @param networkOrGroupOfLinesRef reference to a Network or a group of lines.
     * @param netexEntitiesIndex       index of NeTEx entities found in the common file.
     * @return the network itself or the network to which the group of lines belong.
     */
    public static Network findNetwork(String networkOrGroupOfLinesRef, NetexEntitiesIndex netexEntitiesIndex) {
        Network network = netexEntitiesIndex.getNetworkIndex().get(networkOrGroupOfLinesRef);
        if (network != null) {
            return network;
        } else {
            return netexEntitiesIndex.getNetworkIndex()
                    .getAll()
                    .stream()
                    .filter(n -> n.getGroupsOfLines() != null)
                    .filter(n -> n.getGroupsOfLines()
                            .getGroupOfLines()
                            .stream()
                            .anyMatch(groupOfLine -> groupOfLine.getId().equals(networkOrGroupOfLinesRef)))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
