package com.keeply.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JwtProviderTest {

  private static final Instant NOW = Instant.parse("2026-10-11T06:10:00.987Z");
  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
  private static final String SECRET = "keeply-jwt-test-secret-at-least-32-bytes-long";
  private static final String SESSION_ID = "2391fdbb-16af-489b-9414-631ab841de38";
  private static final String TOKEN_ID = "d4102099-3b66-45f9-b237-dd04491c38f4";
  private static final long ACCESS_LIFETIME_MILLIS = 7_200_000L;
  private static final long REFRESH_LIFETIME_MILLIS = 1_209_600_000L;

  private JwtProperties jwtProperties;
  private Clock clock;
  private JwtProvider jwtProvider;

  @BeforeEach
  void setUp() {
    jwtProperties = new JwtProperties();
    jwtProperties.setSecret(SECRET);
    jwtProperties.setAccessTokenExpiration(ACCESS_LIFETIME_MILLIS);
    jwtProperties.setRefreshTokenExpiration(REFRESH_LIFETIME_MILLIS);
    clock = Clock.fixed(NOW, ZONE);
    jwtProvider = new JwtProvider(jwtProperties, clock);
  }

  @Test
  void generatesDistinctRefreshTokensAtSameInstantAndPreservesSession() {
    String firstToken = jwtProvider.generateRefreshToken(1L, SESSION_ID);
    String secondToken = jwtProvider.generateRefreshToken(1L, SESSION_ID);
    Claims firstClaims = readClaims(firstToken);
    Claims secondClaims = readClaims(secondToken);

    assertThat(firstToken).isNotEqualTo(secondToken);
    assertThat(firstClaims.getId()).isNotEqualTo(secondClaims.getId());
    assertThat(UUID.fromString(firstClaims.getId()).version()).isEqualTo(4);
    assertThat(firstClaims.getIssuedAt())
        .isEqualTo(Date.from(Instant.parse("2026-10-11T06:10:00Z")));
    assertThat(firstClaims.getExpiration()).isEqualTo(secondClaims.getExpiration());
    assertThat(jwtProvider.parseRefreshToken(firstToken))
        .isEqualTo(new RefreshTokenClaims(1L, SESSION_ID, firstClaims.getExpiration().toInstant()));
    assertThat(jwtProvider.parseRefreshToken(secondToken).sessionId()).isEqualTo(SESSION_ID);
  }

  @Test
  void generatesDistinctAccessTokensAndRetainsAccessAuthentication() {
    String firstToken = jwtProvider.generateAccessToken(1L);
    String secondToken = jwtProvider.generateAccessToken(1L);

    assertThat(firstToken).isNotEqualTo(secondToken);
    assertThat(jwtProvider.validateToken(firstToken)).isTrue();
    assertThat(jwtProvider.isAccessToken(firstToken)).isTrue();
    assertThat(jwtProvider.getUserIdFromToken(firstToken)).isEqualTo(1L);
    assertThat(readClaims(firstToken).get("sessionId")).isNull();
    assertThat(readClaims(firstToken).getExpiration().toInstant())
        .isEqualTo(Instant.parse("2026-10-11T08:10:00Z"));
    assertThatThrownBy(() -> jwtProvider.parseRefreshToken(firstToken))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void legacyRefreshGenerationProducesValidNewSession() {
    String firstToken = jwtProvider.generateRefreshToken(1L);
    String secondToken = jwtProvider.generateRefreshToken(1L);

    assertThat(jwtProvider.parseRefreshToken(firstToken).sessionId())
        .isNotEqualTo(jwtProvider.parseRefreshToken(secondToken).sessionId());
    assertThat(jwtProvider.isAccessToken(firstToken)).isFalse();
  }

  @Test
  void alignsExpiryHelperWithSignedExpirationAndApplicationClock() {
    String token = jwtProvider.generateRefreshToken(1L, SESSION_ID);

    assertThat(jwtProvider.getRefreshTokenExpiryDate())
        .isEqualTo(LocalDateTime.ofInstant(jwtProvider.parseRefreshToken(token).expiresAt(), ZONE));
  }

  @Test
  void rejectsExpirationAtExactBoundaryAndAfterBoundary() {
    String token = jwtProvider.generateRefreshToken(1L, SESSION_ID);
    Instant expiration = jwtProvider.parseRefreshToken(token).expiresAt();
    JwtProvider beforeExpiry =
        new JwtProvider(jwtProperties, Clock.fixed(expiration.minusMillis(1), ZONE));
    JwtProvider atExpiry = new JwtProvider(jwtProperties, Clock.fixed(expiration, ZONE));
    JwtProvider afterExpiry =
        new JwtProvider(jwtProperties, Clock.fixed(expiration.plusMillis(1), ZONE));

    assertThat(beforeExpiry.parseRefreshToken(token).userId()).isEqualTo(1L);
    assertThatThrownBy(() -> atExpiry.parseRefreshToken(token))
        .isInstanceOf(ExpiredJwtException.class);
    assertThatThrownBy(() -> afterExpiry.parseRefreshToken(token))
        .isInstanceOf(ExpiredJwtException.class);
    assertThat(afterExpiry.validateToken(token)).isFalse();
  }

  @Test
  void rejectsAccessTokenAtExactExpirationBoundary() {
    String token = jwtProvider.generateAccessToken(1L);
    Instant expiration = readClaims(token).getExpiration().toInstant();
    JwtProvider beforeExpiry =
        new JwtProvider(jwtProperties, Clock.fixed(expiration.minusMillis(1), ZONE));
    JwtProvider atExpiry = new JwtProvider(jwtProperties, Clock.fixed(expiration, ZONE));

    assertThat(beforeExpiry.validateToken(token)).isTrue();
    assertThat(atExpiry.validateToken(token)).isFalse();
    assertThatThrownBy(() -> atExpiry.isAccessToken(token)).isInstanceOf(ExpiredJwtException.class);
    assertThatThrownBy(() -> atExpiry.getUserIdFromToken(token))
        .isInstanceOf(ExpiredJwtException.class);
  }

  @Test
  void rejectsMalformedTypeBeforeAccessAuthentication() {
    String token =
        Jwts.builder()
            .subject("1")
            .claim("type", 123)
            .issuedAt(Date.from(NOW.minusSeconds(1)))
            .expiration(Date.from(NOW.plusSeconds(60)))
            .signWith(getSecretKey())
            .compact();

    assertThat(jwtProvider.validateToken(token)).isFalse();
    assertThatThrownBy(() -> jwtProvider.parseRefreshToken(token)).isInstanceOf(JwtException.class);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"access", "REFRESH", "unexpected"})
  void rejectsSignedWrongOrMissingType(String type) {
    String token =
        createSignedToken(
            "1", type, SESSION_ID, TOKEN_ID, NOW.minusSeconds(1), NOW.plusSeconds(60));

    assertThatThrownBy(() -> jwtProvider.parseRefreshToken(token)).isInstanceOf(JwtException.class);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"0", "-1", "abc", "9223372036854775808"})
  void rejectsInvalidSubjectBeforeAuthentication(String subject) {
    String token =
        createSignedToken(
            subject, "refresh", SESSION_ID, TOKEN_ID, NOW.minusSeconds(1), NOW.plusSeconds(60));

    assertThatThrownBy(() -> jwtProvider.parseRefreshToken(token)).isInstanceOf(JwtException.class);
    assertThatThrownBy(() -> jwtProvider.getUserIdFromToken(token))
        .isInstanceOf(JwtException.class);
    assertThat(jwtProvider.validateToken(token)).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"invalid", "1-1-1-1-1", "2391FDBB-16AF-489B-9414-631AB841DE38"})
  void rejectsMissingOrNonCanonicalSessionId(String sessionId) {
    String token =
        createSignedToken(
            "1", "refresh", sessionId, TOKEN_ID, NOW.minusSeconds(1), NOW.plusSeconds(60));

    assertThatThrownBy(() -> jwtProvider.parseRefreshToken(token))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> jwtProvider.generateRefreshToken(1L, sessionId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"invalid", "1-1-1-1-1", "D4102099-3B66-45F9-B237-DD04491C38F4"})
  void rejectsMissingOrNonCanonicalTokenId(String tokenId) {
    String token =
        createSignedToken(
            "1", "refresh", SESSION_ID, tokenId, NOW.minusSeconds(1), NOW.plusSeconds(60));

    assertThatThrownBy(() -> jwtProvider.parseRefreshToken(token))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsMissingOrInvalidTimestampClaims() {
    String noIssuedAt =
        createSignedToken("1", "refresh", SESSION_ID, TOKEN_ID, null, NOW.plusSeconds(60));
    String noExpiresAt = createSignedToken("1", "refresh", SESSION_ID, TOKEN_ID, NOW, null);
    String reversedTimestamps =
        createSignedToken(
            "1", "refresh", SESSION_ID, TOKEN_ID, NOW.plusSeconds(120), NOW.plusSeconds(60));

    assertThatThrownBy(() -> jwtProvider.parseRefreshToken(noIssuedAt))
        .isInstanceOf(JwtException.class);
    assertThatThrownBy(() -> jwtProvider.parseRefreshToken(noExpiresAt))
        .isInstanceOf(JwtException.class);
    assertThatThrownBy(() -> jwtProvider.parseRefreshToken(reversedTimestamps))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void rejectsSignedLegacyRefreshWithoutSessionClaim() {
    String token =
        createSignedToken("1", "refresh", null, TOKEN_ID, NOW.minusSeconds(1), NOW.plusSeconds(60));

    assertThatThrownBy(() -> jwtProvider.parseRefreshToken(token))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsTamperedSignatureBeforeReturningClaims() {
    String token = jwtProvider.generateRefreshToken(1L, SESSION_ID);
    int signatureStart = token.lastIndexOf('.') + 1;
    char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
    String tamperedToken =
        token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);

    assertThatThrownBy(() -> jwtProvider.parseRefreshToken(tamperedToken))
        .isInstanceOf(JwtException.class);
    assertThat(jwtProvider.validateToken(tamperedToken)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(longs = {0L, -1L})
  void rejectsNonPositiveUserIdAtGeneration(long userId) {
    assertThatThrownBy(() -> jwtProvider.generateAccessToken(userId))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> jwtProvider.generateRefreshToken(userId, SESSION_ID))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullUserIdAtGeneration() {
    assertThatThrownBy(() -> jwtProvider.generateAccessToken(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private Claims readClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSecretKey())
        .clock(() -> Date.from(clock.instant()))
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private String createSignedToken(
      String subject,
      String type,
      String sessionId,
      String tokenId,
      Instant issuedAt,
      Instant expiresAt) {
    return Jwts.builder()
        .subject(subject)
        .claim("type", type)
        .claim("sessionId", sessionId)
        .id(tokenId)
        .issuedAt(issuedAt == null ? null : Date.from(issuedAt))
        .expiration(expiresAt == null ? null : Date.from(expiresAt))
        .signWith(getSecretKey())
        .compact();
  }

  private SecretKey getSecretKey() {
    return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
  }
}
