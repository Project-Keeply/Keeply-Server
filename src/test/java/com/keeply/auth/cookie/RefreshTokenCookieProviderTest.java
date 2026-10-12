package com.keeply.auth.cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieProviderTest {

  private static final Instant NOW = Instant.parse("2026-10-11T00:00:00.750Z");

  private RefreshTokenCookieProperties cookieProperties;
  private RefreshTokenCookieProvider cookieProvider;

  @BeforeEach
  void setUp() {
    cookieProperties = new RefreshTokenCookieProperties();
    cookieProvider =
        new RefreshTokenCookieProvider(cookieProperties, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createsHttpOnlyHostCookieWithLocalDefaults() {
    ResponseCookie cookie = cookieProvider.createCookie("test-refresh-token", NOW.plusSeconds(60));

    assertThat(cookie.getName()).isEqualTo("keeply_refresh_token");
    assertThat(cookie.getValue()).isEqualTo("test-refresh-token");
    assertThat(cookie.getPath()).isEqualTo("/auth");
    assertThat(cookie.getDomain()).isNull();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isSecure()).isFalse();
    assertThat(cookie.getSameSite()).isEqualTo("Lax");
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(60));
  }

  @Test
  void floorsLifetimeFromClockWithoutExtendingJwtExpiration() {
    ResponseCookie cookie =
        cookieProvider.createCookie("test-refresh-token", Instant.parse("2026-10-11T00:01:01Z"));

    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(60));
    assertThat(NOW.plus(cookie.getMaxAge())).isBefore(Instant.parse("2026-10-11T00:01:01Z"));
  }

  @Test
  void rejectsExpiredTokenAndUnusableSubSecondLifetime() {
    assertThatThrownBy(() -> cookieProvider.createCookie("test-refresh-token", NOW.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> cookieProvider.createCookie("test-refresh-token", NOW))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> cookieProvider.createCookie("test-refresh-token", NOW.plusMillis(999)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reusesIdenticalScopeAndSecuritySettingsWhenDeletingCookie() {
    cookieProperties.setSecure(true);
    cookieProperties.setSameSite("None");
    ResponseCookie issuedCookie =
        cookieProvider.createCookie("test-refresh-token", NOW.plusSeconds(60));
    ResponseCookie expiredCookie = cookieProvider.createExpiredCookie();

    assertThat(expiredCookie.getName()).isEqualTo(issuedCookie.getName());
    assertThat(expiredCookie.getPath()).isEqualTo(issuedCookie.getPath());
    assertThat(expiredCookie.getDomain()).isEqualTo(issuedCookie.getDomain());
    assertThat(expiredCookie.getSameSite()).isEqualTo("None");
    assertThat(expiredCookie.isSecure()).isTrue();
    assertThat(expiredCookie.isHttpOnly()).isTrue();
    assertThat(expiredCookie.getValue()).isEmpty();
    assertThat(expiredCookie.getMaxAge()).isEqualTo(Duration.ZERO);
    assertThat(expiredCookie.toString()).contains("Max-Age=0");
  }
}
