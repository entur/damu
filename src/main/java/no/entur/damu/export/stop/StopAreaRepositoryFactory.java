package no.entur.damu.export.stop;

/**
 * Factory for creating a stop area repository
 */
public interface StopAreaRepositoryFactory {

    /**
     * Return an initialized instance if a stop area repository.
     *
     * @return an initialized instance if a stop area repository.
     */
    StopAreaRepository getStopAreaRepository();
}
