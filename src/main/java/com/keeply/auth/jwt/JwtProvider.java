package com.keeply.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtProvider {

  private final JwtProperties jwtProperties;

  public String generateAccessToken(Long userId) {
    return generateToken(userId, jwtProperties.getAccessTokenExpiration());
  }

  public String generateRefreshToken(Long userId) {
    return generateToken(userId, jwtProperties.getRefreshTokenExpiration());
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public Long getUserIdFromToken(String token) {
    Claims claims =
        Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
    return Long.parseLong(claims.getSubject());
  }

  private String generateToken(Long userId, long expiration) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration);
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSecretKey())
        .compact();
  }

  private SecretKey getSecretKey() {
    return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
  }
}
