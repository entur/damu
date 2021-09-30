package no.entur.damu.export.producer;

import no.entur.damu.export.model.RouteTypeEnum;
import no.entur.damu.export.model.TransportModeNameEnum;
import no.entur.damu.export.model.TransportSubModeNameEnum;
import no.entur.damu.export.util.GtfsUtil;
import no.entur.damu.export.util.NetexParserUtils;
import no.entur.damu.export.util.NetworkUtil;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.services.GtfsDao;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.PresentationStructure;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class RouteProducer {

    private final HexBinaryAdapter hexBinaryAdapter;
    private final NetexEntitiesIndex netexEntitiesIndex;
    private final GtfsDao gtfsDao;

    public RouteProducer(NetexEntitiesIndex netexEntitiesIndex, GtfsDao gtfsDao) {
        this.netexEntitiesIndex = netexEntitiesIndex;
        this.gtfsDao = gtfsDao;
        this.hexBinaryAdapter = new HexBinaryAdapter();
    }


    public Route produce(Line line) {
        String lineId = GtfsUtil.toGtfsId(line.getId(), null, true);
        Route route = new Route();

        // route agency
        Network network = NetworkUtil.findNetwork(line.getRepresentedByGroupRef().getRef(), netexEntitiesIndex);
        String authorityId = network.getTransportOrganisationRef().getValue().getRef();
        Agency agency = gtfsDao.getAgencyForId(authorityId);
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
        TransportModeNameEnum transportMode = NetexParserUtils.toTransportModeNameEnum(line.getTransportMode().value());
        TransportSubModeNameEnum transportSubMode = NetexParserUtils.toTransportSubModeNameEnum(line.getTransportSubmode());
        route.setType(RouteTypeEnum.from(transportMode, transportSubMode).getValue());

        // route color
        PresentationStructure presentation = line.getPresentation();
        if (presentation != null) {
            route.setColor(hexBinaryAdapter.marshal(presentation.getColour()));
            route.setTextColor(hexBinaryAdapter.marshal(presentation.getTextColour()));
        }
        return route;
    }
}
