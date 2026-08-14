package no.entur.damu;

import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * The application as the tests boot it.
 *
 * <p>The explicit ComponentScan has to repeat the two filters SpringBootApplication would otherwise
 * contribute. Without TypeExcludeFilter every {@code @TestConfiguration} under {@code no.entur.damu}
 * applies to every test that boots this class, so one test's doubles silently replace another test's
 * beans.
 */
@SpringBootApplication
@ComponentScan(
  excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = App.class),
    @ComponentScan.Filter(
      type = FilterType.CUSTOM,
      classes = TypeExcludeFilter.class
    ),
    @ComponentScan.Filter(
      type = FilterType.CUSTOM,
      classes = AutoConfigurationExcludeFilter.class
    ),
  }
)
public class TestApp extends App {}
