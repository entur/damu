package no.entur.damu.stop;

import java.io.InputStream;
import java.util.UUID;
import no.entur.damu.DamuMdc;
import no.entur.damu.services.MardukBlobStoreService;
import org.entur.netex.gtfs.export.stop.StopAreaRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Keeps the stop area repository current.
 *
 * <p>The startup refresh blocks {@code ApplicationReadyEvent}, so the pod does not report ready while
 * the stop areas are still loading. Liveness is already UP by then, so this cannot restart the pod.
 *
 * <p>Readiness is not conditional on the load having <em>succeeded</em>: a missing or unreadable stop
 * file is logged and the pod goes ready anyway, with an empty repository, until the next scheduled
 * refresh. That matches the Camel version, whose quartz route logged and moved on, and it is the reason
 * the pod does not CrashLoop when the file is absent. Exports in that window fail on
 * {@code getStopAreaRepository()}. The PubSub consumer also starts earlier, on
 * {@code ContextRefreshedEvent}, so a request arriving mid-load hits the same thing.
 */
@Service
public class StopAreaRefreshService {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StopAreaRefreshService.class
  );

  private final MardukBlobStoreService mardukBlobStoreService;
  private final StopAreaRepositoryFactory stopAreaRepositoryFactory;
  private final String stopExportFilename;

  public StopAreaRefreshService(
    MardukBlobStoreService mardukBlobStoreService,
    StopAreaRepositoryFactory stopAreaRepositoryFactory,
    @Value(
      "${damu.netex.stop.full.filename:tiamat/CurrentAndFuture_latest.zip}"
    ) String stopExportFilename
  ) {
    this.mardukBlobStoreService = mardukBlobStoreService;
    this.stopAreaRepositoryFactory = stopAreaRepositoryFactory;
    this.stopExportFilename = stopExportFilename;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void refreshStopsAtStartup() {
    // Contained: an exception escaping an ApplicationReadyEvent listener aborts the application.
    try {
      refreshStops();
    } catch (Exception e) {
      LOGGER.error(
        "Failed to load the stop areas at startup. GTFS exports will fail until the next refresh.",
        e
      );
    }
  }

  @Scheduled(cron = "${damu.netex.stop.cache.refresh.cron:0 0 3 * * *}")
  public void refreshStops() {
    DamuMdc.clear();
    DamuMdc.setCorrelationId(UUID.randomUUID().toString());
    LOGGER.info("Refreshing stop areas.");
    InputStream stopDataset = mardukBlobStoreService.getBlob(
      stopExportFilename
    );
    if (stopDataset == null) {
      LOGGER.error("NeTEx Stopfile not found");
      return;
    }
    stopAreaRepositoryFactory.refreshStopAreaRepository(stopDataset);
    LOGGER.info("Refreshed stop areas.");
  }
}
