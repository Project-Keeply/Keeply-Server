package com.keeply.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

  private static final ZoneId APPLICATION_ZONE_ID = ZoneId.of("Asia/Seoul");

  @Bean
  public Clock clock() {
    return Clock.system(APPLICATION_ZONE_ID);
  }
}
