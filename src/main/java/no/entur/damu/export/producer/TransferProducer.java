package no.entur.damu.export.producer;

import no.entur.damu.export.util.StopUtil;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Transfer;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsDao;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferProducer.class);

    private static final int TRANSFER_RECOMMENDED = 0;
    private static final int TRANSFER_TIMED = 1;
    private static final int TRANSFER_MINIMAL = 2;
    private static final int TRANSFER_NOT_ALLOWED = 3;

    private final Agency agency;
    private final NetexEntitiesIndex netexTimetableEntitiesIndex;
    private final GtfsDao gtfsDao;

    public TransferProducer(Agency agency, NetexEntitiesIndex netexTimetableEntitiesIndex, GtfsDao gtfsDao) {
        this.agency = agency;
        this.netexTimetableEntitiesIndex = netexTimetableEntitiesIndex;
        this.gtfsDao = gtfsDao;
    }


    public Transfer produce(ServiceJourneyInterchange serviceJourneyInterchange) {
        Transfer transfer = new Transfer();

        String fromServiceJourneyId = serviceJourneyInterchange.getFromJourneyRef().getRef();
        Trip fromTrip = getGtfsTripFromServiceJourneyId(fromServiceJourneyId);
        transfer.setFromTrip(fromTrip);

        String toServiceJourneyId = serviceJourneyInterchange.getToJourneyRef().getRef();
        Trip toTrip = getGtfsTripFromServiceJourneyId(toServiceJourneyId);
        transfer.setToTrip(toTrip);

        String fromScheduledStopPointId = serviceJourneyInterchange.getFromPointRef().getRef();
        Stop fromStop = StopUtil.getGtfsStopFromScheduledStopPointId(fromScheduledStopPointId, netexTimetableEntitiesIndex, gtfsDao);
        transfer.setFromStop(fromStop);

        String toScheduledStopPointId = serviceJourneyInterchange.getFromPointRef().getRef();
        Stop toStop = StopUtil.getGtfsStopFromScheduledStopPointId(toScheduledStopPointId, netexTimetableEntitiesIndex, gtfsDao);
        transfer.setToStop(toStop);

        if (Boolean.TRUE.equals(serviceJourneyInterchange.isGuaranteed())) {
            transfer.setTransferType(TRANSFER_TIMED);
        } else if (serviceJourneyInterchange.getMinimumTransferTime() != null) {
            transfer.setTransferType(TRANSFER_MINIMAL);
            transfer.setMinTransferTime((int) (serviceJourneyInterchange.getMinimumTransferTime().getSeconds()));
        } else if (serviceJourneyInterchange.getPriority() != null && serviceJourneyInterchange.getPriority().intValueExact() < 0) {
            transfer.setTransferType(TRANSFER_NOT_ALLOWED);
        } else {
            transfer.setTransferType(TRANSFER_RECOMMENDED);
        }


        return transfer;

    }

    private Trip getGtfsTripFromServiceJourneyId(String serviceJourneyId) {
        ServiceJourney fromServiceJourney = netexTimetableEntitiesIndex.getServiceJourneyIndex().get(serviceJourneyId);
        AgencyAndId agencyAndId = new AgencyAndId();
        agencyAndId.setId(fromServiceJourney.getId());
        agencyAndId.setAgencyId(agency.getId());
        return gtfsDao.getTripForId(agencyAndId);
    }

}
