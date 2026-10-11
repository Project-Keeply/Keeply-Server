package com.keeply.auth.service;

import com.keeply.auth.dto.TokenIssueResult;
import com.keeply.auth.entity.RefreshToken;
import com.keeply.auth.jwt.JwtProvider;
import com.keeply.auth.jwt.RefreshTokenClaims;
import com.keeply.auth.kakao.KakaoUserInfoResponse;
import com.keeply.auth.repository.RefreshTokenRepository;
import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.user.entity.User;
import com.keeply.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthAccountServiceImpl implements AuthAccountService {
  private final JwtProvider jwtProvider;
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final Clock clock;

  @Override
  @Transactional
  public TokenIssueResult processLogin(KakaoUserInfoResponse userInfo) {
    Long kakaoUserId = userInfo.getId();
    if (kakaoUserId == null) {
      throw new CustomException(ErrorCode.KAKAO_AUTH_FAILED);
    }
    String nickname = resolveNickname(userInfo.getNickname());
    User user =
        userRepository
            .findByKakaoIdForUpdate(kakaoUserId.toString())
            .map(
                existing -> {
                  if (existing.isDeleted()) {
                    existing.restore();
                  }
                  existing.syncFromKakao(nickname, userInfo.getProfileImageUrl());
                  return existing;
                })
            .orElseGet(
                () ->
                    userRepository.saveAndFlush(
                        User.builder()
                            .kakaoId(kakaoUserId.toString())
                            .name(nickname)
                            .profileImageUrl(userInfo.getProfileImageUrl())
                            .build()));

    String sessionId = UUID.randomUUID().toString();
    TokenIssueResult result = createTokens(user.getId(), sessionId);
    LocalDateTime expiryDate =
        LocalDateTime.ofInstant(result.refreshTokenExpiresAt(), clock.getZone());
    RefreshToken savedToken = refreshTokenRepository.findByUser(user).orElse(null);
    if (savedToken == null) {
      refreshTokenRepository.save(
          RefreshToken.builder()
              .user(user)
              .sessionId(sessionId)
              .tokenHash(hashToken(result.refreshToken()))
              .expiryDate(expiryDate)
              .build());
    } else {
      savedToken.updateSession(sessionId, hashToken(result.refreshToken()), expiryDate);
    }
    return result;
  }

  @Override
  @Transactional
  public TokenIssueResult rotateRefreshToken(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new CustomException(ErrorCode.REFRESH_TOKEN_MISSING);
    }
    RefreshTokenClaims claims = parseRefreshToken(refreshToken);
    User user =
        userRepository
            .findByIdForUpdate(claims.userId())
            .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_INVALID));
    // 잠금 대기 중 만료된 JWT는 DB 상태를 읽거나 변경하기 전에 거부한다.
    claims = parseRefreshToken(refreshToken);
    if (user.isDeleted()) {
      throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
    }
    RefreshToken savedToken =
        refreshTokenRepository
            .findByUser(user)
            .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_INVALID));
    if (!savedToken.getSessionId().equals(claims.sessionId())) {
      throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
    }
    if (!savedToken.getExpiryDate().isAfter(LocalDateTime.now(clock))) {
      throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }
    if (!savedToken.getTokenHash().equals(hashToken(refreshToken))) {
      throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
    }
    TokenIssueResult result = createTokens(user.getId(), claims.sessionId());
    savedToken.updateToken(
        hashToken(result.refreshToken()),
        LocalDateTime.ofInstant(result.refreshTokenExpiresAt(), clock.getZone()));
    return result;
  }

  @Override
  @Transactional
  public void logoutSession(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return;
    }
    RefreshTokenClaims claims;
    try {
      claims = jwtProvider.parseRefreshToken(refreshToken);
    } catch (JwtException | IllegalArgumentException exception) {
      // 잘못된 쿠키는 멱등 성공이지만 DB/잠금 장애는 숨기지 않는다.
      return;
    }
    Optional<User> user = userRepository.findByIdForUpdate(claims.userId());
    if (user.isEmpty()) {
      return;
    }
    // 잠금 대기가 끝난 시점에도 서명된 토큰의 폐기 권한이 유효해야 한다.
    try {
      jwtProvider.parseRefreshToken(refreshToken);
    } catch (JwtException | IllegalArgumentException exception) {
      return;
    }
    refreshTokenRepository
        .findByUser(user.get())
        .filter(token -> token.getSessionId().equals(claims.sessionId()))
        .ifPresent(refreshTokenRepository::delete);
  }

  @Override
  @Transactional
  public void logout(Long userId) {
    User user =
        userRepository
            .findByIdForUpdate(Objects.requireNonNull(userId))
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    refreshTokenRepository.deleteByUser(user);
  }

  private TokenIssueResult createTokens(Long userId, String sessionId) {
    String accessToken = jwtProvider.generateAccessToken(userId);
    String refreshToken = jwtProvider.generateRefreshToken(userId, sessionId);
    return new TokenIssueResult(
        accessToken, refreshToken, jwtProvider.parseRefreshToken(refreshToken).expiresAt());
  }

  private RefreshTokenClaims parseRefreshToken(String token) {
    try {
      return jwtProvider.parseRefreshToken(token);
    } catch (ExpiredJwtException exception) {
      throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
    } catch (JwtException | IllegalArgumentException exception) {
      throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
    }
  }

  private String resolveNickname(String nickname) {
    return nickname == null || nickname.isBlank() ? "사용자" : nickname;
  }

  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }
}
