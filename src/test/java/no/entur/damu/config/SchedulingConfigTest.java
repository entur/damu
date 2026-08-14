package no.entur.damu.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.entur.damu.DamuPipelineTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskHolder;

/**
 * spring-cloud-gcp registers several {@code TaskScheduler} beans, so Boot's scheduling
 * auto-configuration backs off and {@link SchedulingConfig} has to supply the one {@code @Scheduled}
 * looks for by name.
 */
class SchedulingConfigTest extends DamuPipelineTestBase {

  @Autowired
  private ApplicationContext applicationContext;

  @Value("${spring.task.scheduling.pool.size}")
  private int configuredPoolSize;

  @Test
  void scheduledTasksRunOnTheConfiguredPool() {
    TaskScheduler scheduler = applicationContext.getBean(
      "taskScheduler",
      TaskScheduler.class
    );

    ThreadPoolTaskScheduler threadPool = assertInstanceOf(
      ThreadPoolTaskScheduler.class,
      scheduler
    );
    assertEquals(
      configuredPoolSize,
      threadPool.getScheduledThreadPoolExecutor().getCorePoolSize(),
      "spring.task.scheduling.pool.size must reach the scheduler @Scheduled uses"
    );
  }

  /**
   * The pool size is one thread per {@code @Scheduled} method: the stop place cache refresh and the
   * stop GTFS export. Nothing but this enforces it.
   */
  @Test
  void everyScheduledMethodHasAThread() {
    ScheduledTaskHolder taskHolder = applicationContext.getBean(
      ScheduledTaskHolder.class
    );

    assertTrue(
      taskHolder.getScheduledTasks().size() <= configuredPoolSize,
      "every @Scheduled method needs a thread, or one blocks the other"
    );
  }
}
