package com.keeply.auth.service;

import com.keeply.auth.dto.LoginResponse;
import com.keeply.auth.entity.RefreshToken;
import com.keeply.auth.jwt.JwtProvider;
import com.keeply.auth.kakao.KakaoUserInfoResponse;
import com.keeply.auth.repository.RefreshTokenRepository;
import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.user.entity.User;
import com.keeply.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthAccountService {

  private final JwtProvider jwtProvider;
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;

  @Transactional
  public LoginResponse processLogin(KakaoUserInfoResponse userInfo) {
    Long kakaoUserId = userInfo.getId();
    if (kakaoUserId == null) {
      throw new CustomException(ErrorCode.KAKAO_AUTH_FAILED);
    }
    String kakaoId = kakaoUserId.toString();
    String nickname = resolveNickname(userInfo.getNickname());
    User user =
        userRepository
            .findByKakaoId(kakaoId)
            .map(
                existing -> {
                  if (existing.isDeleted()) {
                    existing.restore();
                  }
                  existing.syncFromKakao(nickname, userInfo.getProfileImageUrl());
                  return existing;
                })
            .orElseGet(
                () -> {
                  User newUser =
                      User.builder()
                          .kakaoId(kakaoId)
                          .name(nickname)
                          .profileImageUrl(userInfo.getProfileImageUrl())
                          .build();
                  return userRepository.save(Objects.requireNonNull(newUser));
                });

    String accessToken = jwtProvider.generateAccessToken(user.getId());
    String refreshToken = jwtProvider.generateRefreshToken(user.getId());

    saveOrUpdateRefreshToken(user, refreshToken);

    return LoginResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();
  }

  @Transactional
  public LoginResponse rotateRefreshToken(String refreshToken) {
    if (!jwtProvider.validateToken(refreshToken)) {
      throw new CustomException(ErrorCode.INVALID_TOKEN);
    }

    Long userId = jwtProvider.getUserIdFromToken(refreshToken);
    User user =
        userRepository
            .findById(Objects.requireNonNull(userId))
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    RefreshToken savedToken =
        refreshTokenRepository
            .findByUser(user)
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

    if (!savedToken.getTokenHash().equals(hashToken(refreshToken))) {
      throw new CustomException(ErrorCode.INVALID_TOKEN);
    }

    String newAccessToken = jwtProvider.generateAccessToken(userId);
    String newRefreshToken = jwtProvider.generateRefreshToken(userId);

    savedToken.updateToken(hashToken(newRefreshToken), jwtProvider.getRefreshTokenExpiryDate());

    return LoginResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken)
        .build();
  }

  private void saveOrUpdateRefreshToken(User user, String refreshToken) {
    String hashedToken = hashToken(refreshToken);
    LocalDateTime expiryDate = jwtProvider.getRefreshTokenExpiryDate();

    RefreshToken token =
        refreshTokenRepository
            .findByUser(user)
            .map(
                existing -> {
                  existing.updateToken(hashedToken, expiryDate);
                  return existing;
                })
            .orElseGet(
                () ->
                    RefreshToken.builder()
                        .user(user)
                        .tokenHash(hashedToken)
                        .expiryDate(expiryDate)
                        .build());
    refreshTokenRepository.save(Objects.requireNonNull(token));
  }

  @Transactional
  public void logout(Long userId) {
    User user =
        userRepository
            .findById(Objects.requireNonNull(userId))
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    refreshTokenRepository.deleteByUser(user);
  }

  private String resolveNickname(String nickname) {
    if (nickname == null || nickname.isBlank()) {
      return "사용자";
    }
    return nickname;
  }

  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        hexString.append(String.format("%02x", b));
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }
}
