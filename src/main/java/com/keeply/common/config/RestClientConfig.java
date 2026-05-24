package com.keeply.common.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  private static final Duration KAKAO_CONNECT_TIMEOUT = Duration.ofSeconds(3);
  private static final Duration KAKAO_READ_TIMEOUT = Duration.ofSeconds(5);

  @Bean
  public RestClient kakaoRestClient() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(KAKAO_CONNECT_TIMEOUT);
    factory.setReadTimeout(KAKAO_READ_TIMEOUT);
    return RestClient.builder().requestFactory(factory).build();
  }
}
