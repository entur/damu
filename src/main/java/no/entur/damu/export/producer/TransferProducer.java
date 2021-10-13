package no.entur.damu.export.producer;

import no.entur.damu.export.repository.GtfsDatasetRepository;
import no.entur.damu.export.repository.NetexDatasetRepository;
import no.entur.damu.export.util.StopUtil;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Transfer;
import org.onebusaway.gtfs.model.Trip;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

public class TransferProducer {

    private static final int TRANSFER_RECOMMENDED = 0;
    private static final int TRANSFER_TIMED = 1;
    private static final int TRANSFER_MINIMAL = 2;
    private static final int TRANSFER_NOT_ALLOWED = 3;

    private final Agency agency;
    private final NetexDatasetRepository netexDatasetRepository;
    private final GtfsDatasetRepository gtfsDatasetRepository;

    public TransferProducer(NetexDatasetRepository netexDatasetRepository, GtfsDatasetRepository gtfsDatasetRepository) {
        this.agency = gtfsDatasetRepository.getDefaultAgency();
        this.netexDatasetRepository = netexDatasetRepository;
        this.gtfsDatasetRepository = gtfsDatasetRepository;
    }


    public Transfer produce(ServiceJourneyInterchange serviceJourneyInterchange) {
        Transfer transfer = new Transfer();

        String fromServiceJourneyId = serviceJourneyInterchange.getFromJourneyRef().getRef();
        Trip fromTrip = gtfsDatasetRepository.getTripById(fromServiceJourneyId);
        transfer.setFromTrip(fromTrip);

        String toServiceJourneyId = serviceJourneyInterchange.getToJourneyRef().getRef();
        Trip toTrip = gtfsDatasetRepository.getTripById(toServiceJourneyId);
        transfer.setToTrip(toTrip);

        String fromScheduledStopPointId = serviceJourneyInterchange.getFromPointRef().getRef();
        Stop fromStop = StopUtil.getGtfsStopFromScheduledStopPointId(fromScheduledStopPointId, netexDatasetRepository, gtfsDatasetRepository);
        transfer.setFromStop(fromStop);

        String toScheduledStopPointId = serviceJourneyInterchange.getFromPointRef().getRef();
        Stop toStop = StopUtil.getGtfsStopFromScheduledStopPointId(toScheduledStopPointId, netexDatasetRepository, gtfsDatasetRepository);
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


}
