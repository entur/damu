package no.entur.damu.stop;

import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;

/**
 * A repository containing quays and stop places
 */
public interface StopAreaRepository {


    /**
     * Return the Quay identified by its id.
     * @param quayId the quay id.
     * @return the quay identified by this id.
     */
    Quay getQuayById(String quayId);

    /**
     * Return the stop place associated to a given quay.
     * @param quayId the id of the quay
     * @return the stop place that contains that quay.
     */
    StopPlace getStopPlaceByQuayId(String quayId);



}

