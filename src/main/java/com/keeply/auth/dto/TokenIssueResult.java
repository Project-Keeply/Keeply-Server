package com.keeply.auth.dto;

import java.time.Instant;

/** 서비스 내부 발급 결과. API JSON에는 LoginResponse만 반환하고 Refresh Token은 쿠키로 전달한다. */
public record TokenIssueResult(
    String accessToken, String refreshToken, Instant refreshTokenExpiresAt) {}
