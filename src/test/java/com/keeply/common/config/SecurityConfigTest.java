package com.keeply.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://keeply-work.vercel.app",
        "https://keeply-client.vercel.app",
        "http://localhost:5173"
      })
  void corsConfigurationAllowsConfiguredOrigins(String origin) {
    CorsConfiguration corsConfiguration = getCorsConfiguration(origin);

    String allowedOrigin = corsConfiguration.checkOrigin(origin);

    assertThat(allowedOrigin).isEqualTo(origin);
  }

  @Test
  void corsConfigurationRejectsUnconfiguredOrigin() {
    CorsConfiguration corsConfiguration = getCorsConfiguration("https://example.com");

    String allowedOrigin = corsConfiguration.checkOrigin("https://example.com");

    assertThat(allowedOrigin).isNull();
  }

  @Test
  void corsConfigurationAllowsAuthorizationPreflight() {
    CorsConfiguration corsConfiguration = getCorsConfiguration("https://keeply-work.vercel.app");

    assertThat(corsConfiguration.getAllowedHeaders()).contains("*");
    assertThat(corsConfiguration.getAllowedMethods())
        .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    assertThat(corsConfiguration.getAllowCredentials()).isTrue();
  }

  private CorsConfiguration getCorsConfiguration(String origin) {
    CorsProperties corsProperties = new CorsProperties();
    corsProperties.setAllowedOrigins(
        List.of(
            "https://keeply-work.vercel.app",
            "https://keeply-client.vercel.app",
            "http://localhost:5173"));

    SecurityConfig securityConfig = new SecurityConfig(null, null, null, corsProperties);
    CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Origin", origin);

    return corsConfigurationSource.getCorsConfiguration(request);
  }
}
