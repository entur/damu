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

public class RouteProducer {

    private final Agency agency;

    public RouteProducer(Agency agency) {
        this.agency = agency;
    }


    public Route produce(Line line) {
        String lineId = GtfsUtil.toGtfsId(line.getId(), "OST", true);
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

        return route;

    }

}
