package no.entur.damu;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class PipelineTestConfig {

  @Bean
  @Primary
  RecordingPubSubPublisher recordingPubSubPublisher() {
    return new RecordingPubSubPublisher();
  }
}
