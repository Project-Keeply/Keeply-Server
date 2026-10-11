package com.keeply.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtProvider {

  private final JwtProperties jwtProperties;
  private final Clock clock;

  public String generateAccessToken(Long userId) {
    return generateToken(userId, "access", jwtProperties.getAccessTokenExpiration(), null);
  }

  public String generateRefreshToken(Long userId) {
    return generateRefreshToken(userId, UUID.randomUUID().toString());
  }

  public String generateRefreshToken(Long userId, String sessionId) {
    checkUuidClaim(sessionId, "sessionId");
    return generateToken(userId, "refresh", jwtProperties.getRefreshTokenExpiration(), sessionId);
  }

  public LocalDateTime getRefreshTokenExpiryDate() {
    return LocalDateTime.ofInstant(
        getTokenExpiryInstant(jwtProperties.getRefreshTokenExpiration()), clock.getZone());
  }

  public boolean isAccessToken(String token) {
    Claims claims = parseSignedToken(token).getPayload();
    return "access".equals(claims.get("type", String.class));
  }

  public boolean validateToken(String token) {
    try {
      Claims claims = parseSignedToken(token).getPayload();
      parseUserId(claims.getSubject());
      String type = claims.get("type", String.class);
      return "access".equals(type) || "refresh".equals(type);
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public Long getUserIdFromToken(String token) {
    Claims claims = parseSignedToken(token).getPayload();
    return parseUserId(claims.getSubject());
  }

  public RefreshTokenClaims parseRefreshToken(String token) {
    Jws<Claims> signedToken = parseSignedToken(token);
    Claims claims = signedToken.getPayload();
    if (!"refresh".equals(claims.get("type", String.class))) {
      throw new JwtException("Invalid refresh token type");
    }
    Long userId = parseUserId(claims.getSubject());
    String sessionId = claims.get("sessionId", String.class);
    checkUuidClaim(sessionId, "sessionId");
    checkUuidClaim(claims.getId(), "jti");
    Date issuedAt = claims.getIssuedAt();
    Date expiresAt = claims.getExpiration();
    if (issuedAt == null || expiresAt == null || !expiresAt.after(issuedAt)) {
      throw new JwtException("Invalid refresh token timestamps");
    }
    return new RefreshTokenClaims(userId, sessionId, expiresAt.toInstant());
  }

  private Jws<Claims> parseSignedToken(String token) {
    Jws<Claims> signedToken =
        Jwts.parser()
            .verifyWith(getSecretKey())
            .clock(() -> Date.from(clock.instant()))
            .build()
            .parseSignedClaims(token);
    Date expiresAt = signedToken.getPayload().getExpiration();
    if (expiresAt == null) {
      throw new JwtException("Missing token expiration");
    }
    if (!expiresAt.toInstant().isAfter(clock.instant())) {
      throw new ExpiredJwtException(
          signedToken.getHeader(), signedToken.getPayload(), "Token has expired");
    }
    return signedToken;
  }

  private String generateToken(Long userId, String type, long expiration, String sessionId) {
    if (userId == null || userId <= 0) {
      throw new IllegalArgumentException("User ID must be positive");
    }
    Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
    Instant expiresAt = issuedAt.plusMillis(expiration).truncatedTo(ChronoUnit.SECONDS);
    if (!expiresAt.isAfter(issuedAt)) {
      throw new IllegalArgumentException("Token lifetime must be at least one second");
    }
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("type", type)
        .claim("sessionId", sessionId)
        .id(UUID.randomUUID().toString())
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiresAt))
        .signWith(getSecretKey())
        .compact();
  }

  private Instant getTokenExpiryInstant(long expiration) {
    return clock
        .instant()
        .truncatedTo(ChronoUnit.SECONDS)
        .plusMillis(expiration)
        .truncatedTo(ChronoUnit.SECONDS);
  }

  private Long parseUserId(String subject) {
    if (subject == null) {
      throw new JwtException("Missing token subject");
    }
    try {
      Long userId = Long.parseLong(subject);
      if (userId <= 0) {
        throw new JwtException("Token subject must be a positive user ID");
      }
      return userId;
    } catch (NumberFormatException e) {
      throw new JwtException("Invalid token subject", e);
    }
  }

  private void checkUuidClaim(String value, String claimName) {
    if (value == null) {
      throw new IllegalArgumentException("Missing " + claimName);
    }
    if (!UUID.fromString(value).toString().equals(value)) {
      throw new IllegalArgumentException("Invalid " + claimName);
    }
  }

  private SecretKey getSecretKey() {
    return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
  }
}
