package com.keeply.auth.jwt;

import java.time.Instant;

public record RefreshTokenClaims(Long userId, String sessionId, Instant expiresAt) {}
