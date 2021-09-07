package no.entur.damu.producer;

import no.entur.damu.model.RouteTypeEnum;
import no.entur.damu.model.TransportModeNameEnum;
import no.entur.damu.model.TransportSubModeNameEnum;
import no.entur.damu.util.GtfsUtil;
import no.entur.damu.util.NetexParserUtils;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.PresentationStructure;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class RouteProducer {

    private final Agency agency;
    private final HexBinaryAdapter hexBinaryAdapter;

    public RouteProducer(Agency agency) {
        this.agency = agency;
        this.hexBinaryAdapter = new HexBinaryAdapter();
    }


    public Route produce(Line line) {
        String lineId = GtfsUtil.toGtfsId(line.getId(), null, true);
        Route route = new Route();

        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setId(lineId);
        agencyAndId.setAgencyId(agency.getId());
        route.setId(agencyAndId);
        route.setAgency(agency);

        route.setShortName(line.getPublicCode());
        route.setLongName(line.getName().getValue());

        TransportModeNameEnum transportMode = NetexParserUtils.toTransportModeNameEnum(line.getTransportMode().value());
        TransportSubModeNameEnum transportSubMode = NetexParserUtils.toTransportSubModeNameEnum(line.getTransportSubmode());
        route.setType(RouteTypeEnum.from(transportMode, transportSubMode).getValue());

        PresentationStructure presentation = line.getPresentation();
        if (presentation != null) {
            route.setColor(hexBinaryAdapter.marshal(presentation.getColour()));
            route.setTextColor(hexBinaryAdapter.marshal(presentation.getTextColour()));
        }
        return route;
    }
}
