package no.entur.damu.config;

import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * The scheduler behind the {@code @Scheduled} stop place jobs.
 *
 * <p>Spring Boot would normally contribute this, but its auto-configuration backs off when another
 * {@code TaskScheduler} bean already exists, and spring-cloud-gcp registers two of them
 * ({@code pubsubPublisherThreadPool} and {@code globalPubSubSubscriberThreadPoolScheduler}). Without a
 * bean named {@code taskScheduler}, {@code @Scheduled} logs an ambiguity warning on every boot and
 * falls back to a single-threaded executor of its own, which silently ignores
 * {@code spring.task.scheduling.pool.size}.
 */
@Configuration
public class SchedulingConfig {

  @Bean
  ThreadPoolTaskScheduler taskScheduler(
    ThreadPoolTaskSchedulerBuilder builder
  ) {
    return builder.build();
  }
}
