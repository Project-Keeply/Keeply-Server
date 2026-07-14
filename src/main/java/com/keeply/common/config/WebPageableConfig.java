package com.keeply.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class WebPageableConfig {

  private static final int MAX_PAGE_SIZE = 100;

  @Bean
  public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
    return resolver -> resolver.setMaxPageSize(MAX_PAGE_SIZE);
  }
}
