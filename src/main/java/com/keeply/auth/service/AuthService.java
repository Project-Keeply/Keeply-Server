package com.keeply.auth.service;

import com.keeply.auth.dto.LoginResponse;
import com.keeply.auth.entity.RefreshToken;
import com.keeply.auth.jwt.JwtProvider;
import com.keeply.auth.kakao.KakaoAuthClient;
import com.keeply.auth.kakao.KakaoUserInfoResponse;
import com.keeply.auth.repository.RefreshTokenRepository;
import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.user.entity.User;
import com.keeply.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final KakaoAuthClient kakaoAuthClient;
  private final JwtProvider jwtProvider;
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;

  public LoginResponse login(String code) {
    String kakaoAccessToken = kakaoAuthClient.getAccessToken(code).getAccessToken();
    KakaoUserInfoResponse userInfo = kakaoAuthClient.getUserInfo(kakaoAccessToken);
    return processLogin(userInfo);
  }

  @Transactional
  protected LoginResponse processLogin(KakaoUserInfoResponse userInfo) {
    Long kakaoUserId = userInfo.getId();
    if (kakaoUserId == null) {
      throw new CustomException(ErrorCode.KAKAO_AUTH_FAILED);
    }
    String kakaoId = kakaoUserId.toString();
    String nickname = resolveNickname(userInfo.getNickname());
    User user =
        userRepository
            .findByKakaoId(kakaoId)
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

    refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);

    RefreshToken newToken =
        RefreshToken.builder()
            .user(user)
            .tokenHash(hashToken(refreshToken))
            .expiryDate(jwtProvider.getRefreshTokenExpiryDate())
            .build();
    refreshTokenRepository.save(Objects.requireNonNull(newToken));

    return LoginResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();
  }

  @Transactional
  public LoginResponse reissue(String refreshToken) {
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

    refreshTokenRepository.delete(savedToken);
    RefreshToken rotatedToken =
        RefreshToken.builder()
            .user(user)
            .tokenHash(hashToken(newRefreshToken))
            .expiryDate(jwtProvider.getRefreshTokenExpiryDate())
            .build();
    refreshTokenRepository.save(Objects.requireNonNull(rotatedToken));

    return LoginResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken)
        .build();
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
