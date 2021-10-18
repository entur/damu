package no.entur.damu.export.producer;

import no.entur.damu.export.repository.GtfsDatasetRepository;
import no.entur.damu.export.repository.NetexDatasetRepository;
import no.entur.damu.export.util.TransportModeUtils;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.PresentationStructure;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Produce a GTFS Route from a NeTEx Line.
 * Supported GTFS Extension: the route_type field is ammped to GTFS extended route types.
 */
public class DefaultRouteProducer implements RouteProducer {

    private final HexBinaryAdapter hexBinaryAdapter;
    private final NetexDatasetRepository netexDatasetRepository;
    private final GtfsDatasetRepository gtfsDatasetRepository;

    public DefaultRouteProducer(NetexDatasetRepository netexDatasetRepository, GtfsDatasetRepository gtfsDatasetRepository) {
        this.netexDatasetRepository = netexDatasetRepository;
        this.gtfsDatasetRepository = gtfsDatasetRepository;
        this.hexBinaryAdapter = new HexBinaryAdapter();
    }


    @Override
    public Route produce(Line line) {
        String lineId = line.getId();
        Route route = new Route();

        // route agency
        String authorityId = netexDatasetRepository.getAuthorityIdForLine(line);
        Agency agency = gtfsDatasetRepository.getAgencyById(authorityId);
        route.setAgency(agency);

        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setId(lineId);
        agencyAndId.setAgencyId(agency.getId());
        route.setId(agencyAndId);


        // route short and long names
        route.setShortName(line.getPublicCode());
        route.setLongName(line.getName().getValue());

        // route description
        if (line.getDescription() != null) {
            route.setDesc(line.getDescription().getValue());
        }

        // route URL
        route.setUrl(line.getUrl());

        // route type
       route.setType(TransportModeUtils.getGtfsExtendedRouteType(line));

        // route color
        PresentationStructure presentation = line.getPresentation();
        if (presentation != null) {
            route.setColor(hexBinaryAdapter.marshal(presentation.getColour()));
            route.setTextColor(hexBinaryAdapter.marshal(presentation.getTextColour()));
        }
        return route;
    }
}
