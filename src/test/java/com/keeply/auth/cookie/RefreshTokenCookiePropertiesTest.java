package com.keeply.auth.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class RefreshTokenCookiePropertiesTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
          .withUserConfiguration(CookieConfiguration.class);

  @Test
  void bindsDefaultLocalSettings() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          RefreshTokenCookieProperties properties =
              context.getBean(RefreshTokenCookieProperties.class);
          assertThat(properties.isSecure()).isFalse();
          assertThat(properties.getSameSite()).isEqualTo("Lax");
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"Lax", "Strict", "None"})
  void bindsSupportedSameSiteWithSecure(String sameSite) {
    contextRunner
        .withPropertyValues("app.auth-cookie.secure=true", "app.auth-cookie.same-site=" + sameSite)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              RefreshTokenCookieProperties properties =
                  context.getBean(RefreshTokenCookieProperties.class);
              assertThat(properties.isSecure()).isTrue();
              assertThat(properties.getSameSite()).isEqualTo(sameSite);
            });
  }

  @Test
  void rejectsNoneWithoutSecureAtStartup() {
    contextRunner
        .withPropertyValues("app.auth-cookie.secure=false", "app.auth-cookie.same-site=None")
        .run(context -> assertThat(context).hasFailed());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "none", "Unsupported"})
  void rejectsUnsupportedOrBlankSameSiteAtStartup(String sameSite) {
    contextRunner
        .withPropertyValues("app.auth-cookie.secure=true", "app.auth-cookie.same-site=" + sameSite)
        .run(context -> assertThat(context).hasFailed());
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(RefreshTokenCookieProperties.class)
  static class CookieConfiguration {}
}
