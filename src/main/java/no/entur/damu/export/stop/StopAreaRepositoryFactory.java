package no.entur.damu.export.stop;

/**
 * Factory for creating stop area repositories.
 */
public interface StopAreaRepositoryFactory {

    /**
     * Return an initialized instance of a stop area repository.
     * Multiple calls to the method may return different repositories if the underlying implementation allows for refreshing the dataset at runtime.
     *
     * @return an initialized instance of a stop area repository.
     */
    StopAreaRepository getStopAreaRepository();
}
