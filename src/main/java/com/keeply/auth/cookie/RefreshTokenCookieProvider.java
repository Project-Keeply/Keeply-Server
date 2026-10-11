package com.keeply.auth.cookie;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieProvider {

  public static final String NAME = "keeply_refresh_token";
  public static final String PATH = "/auth";

  private final RefreshTokenCookieProperties cookieProperties;
  private final Clock clock;

  public ResponseCookie createCookie(String token, Instant expiresAt) {
    long remainingSeconds = Duration.between(clock.instant(), expiresAt).getSeconds();
    if (remainingSeconds <= 0) {
      throw new IllegalArgumentException("쿠키를 발급할 Refresh Token의 남은 수명이 부족합니다.");
    }
    return createCookieBuilder(token).maxAge(remainingSeconds).build();
  }

  public ResponseCookie createExpiredCookie() {
    return createCookieBuilder("").maxAge(0).build();
  }

  private ResponseCookie.ResponseCookieBuilder createCookieBuilder(String token) {
    return ResponseCookie.from(NAME, token)
        .path(PATH)
        .httpOnly(true)
        .secure(cookieProperties.isSecure())
        .sameSite(cookieProperties.getSameSite());
  }
}
