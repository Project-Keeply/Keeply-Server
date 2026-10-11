package com.keeply.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.keeply.auth.dto.LoginResponse;
import com.keeply.auth.entity.RefreshToken;
import com.keeply.auth.jwt.JwtProperties;
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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AuthAccountServiceImplTest {
  @Mock private UserRepository userRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private KakaoUserInfoResponse userInfo;
  private MutableClock clock;
  private JwtProvider jwtProvider;
  private AuthAccountServiceImpl service;
  private User user;
  private String sessionId;

  @BeforeEach
  void setUp() {
    clock = new MutableClock(Instant.parse("2026-10-11T00:00:00Z"));
    JwtProperties properties = new JwtProperties();
    properties.setSecret("a-secure-256-bit-minimum-test-secret-key-for-keeply");
    properties.setAccessTokenExpiration(7_200_000);
    properties.setRefreshTokenExpiration(1_209_600_000);
    jwtProvider = new JwtProvider(properties, clock);
    service =
        new AuthAccountServiceImpl(jwtProvider, userRepository, refreshTokenRepository, clock);
    user = User.builder().id(7L).kakaoId("123").name("기존 이름").build();
    sessionId = UUID.randomUUID().toString();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void rejectsMissingToken(String token) {
    assertError(() -> service.rotateRefreshToken(token), ErrorCode.REFRESH_TOKEN_MISSING);
    verifyNoInteractions(userRepository, refreshTokenRepository);
  }

  @Test
  void rejectsInvalidOrAccessTokenBeforeReadingDatabase() {
    assertError(() -> service.rotateRefreshToken("invalid"), ErrorCode.REFRESH_TOKEN_INVALID);
    assertError(
        () -> service.rotateRefreshToken(jwtProvider.generateAccessToken(7L)),
        ErrorCode.REFRESH_TOKEN_INVALID);
    verifyNoInteractions(userRepository, refreshTokenRepository);
  }

  @Test
  void rejectsExpiredToken() {
    String token = createRefreshToken();
    clock.instant = clock.instant.plusSeconds(1_209_600);
    assertError(() -> service.rotateRefreshToken(token), ErrorCode.REFRESH_TOKEN_EXPIRED);
    verifyNoInteractions(userRepository, refreshTokenRepository);
  }

  @Test
  void rejectsTokenExpiredWhileWaitingForUserLock() {
    String token = createRefreshToken();
    when(userRepository.findByIdForUpdate(7L))
        .thenAnswer(
            invocation -> {
              clock.instant = clock.instant.plusSeconds(1_209_600);
              return Optional.of(user);
            });
    assertError(() -> service.rotateRefreshToken(token), ErrorCode.REFRESH_TOKEN_EXPIRED);
    verifyNoInteractions(refreshTokenRepository);
  }

  @Test
  void locksUserThenRotatesOnlyOnceAndKeepsSuccessfulTokenUsable() {
    String token = createRefreshToken();
    RefreshToken savedToken = saveToken(token);
    LoginResponse first = service.rotateRefreshToken(token);
    assertThat(first.getRefreshToken()).isNotEqualTo(token);
    assertThat(jwtProvider.parseRefreshToken(first.getRefreshToken()).sessionId())
        .isEqualTo(sessionId);
    assertThat(savedToken.getTokenHash()).isEqualTo(hashToken(first.getRefreshToken()));
    assertThat(savedToken.getExpiryDate())
        .isEqualTo(
            LocalDateTime.ofInstant(
                jwtProvider.parseRefreshToken(first.getRefreshToken()).expiresAt(),
                clock.getZone()));
    assertError(() -> service.rotateRefreshToken(token), ErrorCode.REFRESH_TOKEN_MISMATCH);
    assertThatCode(() -> service.rotateRefreshToken(first.getRefreshToken()))
        .doesNotThrowAnyException();
    InOrder order = inOrder(userRepository, refreshTokenRepository);
    order.verify(userRepository).findByIdForUpdate(7L);
    order.verify(refreshTokenRepository).findByUser(user);
    verify(refreshTokenRepository, never()).delete(any());
  }

  @Test
  void rejectsMissingUser() {
    String token = createRefreshToken();
    when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.empty());
    assertError(() -> service.rotateRefreshToken(token), ErrorCode.REFRESH_TOKEN_INVALID);
    verifyNoInteractions(refreshTokenRepository);
  }

  @Test
  void rejectsDeletedUser() {
    String token = createRefreshToken();
    user.markDeleted();
    when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
    assertError(() -> service.rotateRefreshToken(token), ErrorCode.REFRESH_TOKEN_INVALID);
    verifyNoInteractions(refreshTokenRepository);
  }

  @Test
  void rejectsAbsentSession() {
    String token = createRefreshToken();
    when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
    when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.empty());
    assertError(() -> service.rotateRefreshToken(token), ErrorCode.REFRESH_TOKEN_INVALID);
  }

  @Test
  void rejectsReplacedLoginSessionWithoutChangingIt() {
    String token = createRefreshToken();
    RefreshToken savedToken = saveToken(token);
    String nextSessionId = UUID.randomUUID().toString();
    savedToken.updateSession(nextSessionId, savedToken.getTokenHash(), savedToken.getExpiryDate());
    assertError(() -> service.rotateRefreshToken(token), ErrorCode.REFRESH_TOKEN_INVALID);
    assertThat(savedToken.getSessionId()).isEqualTo(nextSessionId);
  }

  @Test
  void rejectsDatabaseExpiryAtExactBoundary() {
    String token = createRefreshToken();
    RefreshToken savedToken = saveToken(token);
    savedToken.updateToken(savedToken.getTokenHash(), LocalDateTime.now(clock));
    assertError(() -> service.rotateRefreshToken(token), ErrorCode.REFRESH_TOKEN_EXPIRED);
  }

  @Test
  void loginRestoresExistingUserAndReplacesSession() {
    String oldToken = createRefreshToken();
    RefreshToken savedToken = saveTokenForLogin(oldToken);
    user.markDeleted();
    when(userInfo.getId()).thenReturn(123L);
    when(userInfo.getNickname()).thenReturn("카카오 이름");
    when(userRepository.findByKakaoIdForUpdate("123")).thenReturn(Optional.of(user));
    LoginResponse result = service.processLogin(userInfo);
    assertThat(user.isDeleted()).isFalse();
    assertThat(user.getName()).isEqualTo("카카오 이름");
    assertThat(savedToken.getSessionId()).isNotEqualTo(sessionId);
    assertThat(jwtProvider.parseRefreshToken(result.getRefreshToken()).sessionId())
        .isEqualTo(savedToken.getSessionId());
    assertThat(savedToken.getTokenHash()).isEqualTo(hashToken(result.getRefreshToken()));
    verify(userRepository, never()).findByKakaoId(any());
  }

  @Test
  void firstLoginFlushesUniqueUserBeforeCreatingRefreshSession() {
    when(userInfo.getId()).thenReturn(123L);
    when(userRepository.findByKakaoIdForUpdate("123")).thenReturn(Optional.empty());
    when(userRepository.saveAndFlush(any(User.class)))
        .thenAnswer(
            invocation -> {
              User newUser = invocation.getArgument(0);
              assertThat(newUser.getName()).isEqualTo("사용자");
              return user;
            });
    when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.empty());
    LoginResponse result = service.processLogin(userInfo);
    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(captor.capture());
    assertThat(captor.getValue().getSessionId())
        .isEqualTo(jwtProvider.parseRefreshToken(result.getRefreshToken()).sessionId());
    assertThat(captor.getValue().getTokenHash()).isEqualTo(hashToken(result.getRefreshToken()));
    InOrder order = inOrder(userRepository, refreshTokenRepository);
    order.verify(userRepository).findByKakaoIdForUpdate("123");
    order.verify(userRepository).saveAndFlush(any());
    order.verify(refreshTokenRepository).findByUser(user);
  }

  @Test
  void simultaneousFirstLoginUniqueFailureIsPropagatedWithoutRetry() {
    when(userInfo.getId()).thenReturn(123L);
    when(userRepository.findByKakaoIdForUpdate("123")).thenReturn(Optional.empty());
    DataIntegrityViolationException failure =
        new DataIntegrityViolationException("unique conflict");
    when(userRepository.saveAndFlush(any())).thenThrow(failure);
    assertThatThrownBy(() -> service.processLogin(userInfo)).isSameAs(failure);
    verify(userRepository).saveAndFlush(any());
    verifyNoInteractions(refreshTokenRepository);
  }

  @Test
  void logoutWithRotatedOldTokenDeletesCurrentSameSession() {
    String token = createRefreshToken();
    RefreshToken savedToken = saveToken(token);
    service.rotateRefreshToken(token);
    service.logoutSession(token);
    verify(refreshTokenRepository).delete(savedToken);
  }

  @Test
  void oldLoginCannotLogoutNewLogin() {
    String token = createRefreshToken();
    RefreshToken savedToken = saveToken(token);
    savedToken.updateSession(
        UUID.randomUUID().toString(), savedToken.getTokenHash(), savedToken.getExpiryDate());
    service.logoutSession(token);
    verify(refreshTokenRepository, never()).delete(any());
  }

  @Test
  void logoutMissingInvalidAndExpiredTokensIsIdempotent() {
    String expiredToken = createRefreshToken();
    clock.instant = clock.instant.plusSeconds(1_209_600);
    assertThatCode(
            () -> {
              service.logoutSession(null);
              service.logoutSession(" ");
              service.logoutSession("invalid");
              service.logoutSession(expiredToken);
              service.logoutSession(jwtProvider.generateAccessToken(7L));
            })
        .doesNotThrowAnyException();
    verifyNoInteractions(userRepository, refreshTokenRepository);
  }

  @Test
  void logoutPropagatesDatabaseFailure() {
    String token = createRefreshToken();
    RefreshToken savedToken = saveToken(token);
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("database unavailable");
    org.mockito.Mockito.doThrow(failure).when(refreshTokenRepository).delete(savedToken);
    assertThatThrownBy(() -> service.logoutSession(token)).isSameAs(failure);
  }

  private String createRefreshToken() {
    return jwtProvider.generateRefreshToken(7L, sessionId);
  }

  private RefreshToken saveToken(String token) {
    when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
    return saveTokenForLogin(token);
  }

  private RefreshToken saveTokenForLogin(String token) {
    RefreshToken savedToken =
        RefreshToken.builder()
            .user(user)
            .sessionId(sessionId)
            .tokenHash(hashToken(token))
            .expiryDate(
                LocalDateTime.ofInstant(
                    jwtProvider.parseRefreshToken(token).expiresAt(), clock.getZone()))
            .build();
    when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.of(savedToken));
    return savedToken;
  }

  private String hashToken(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new AssertionError(exception);
    }
  }

  private void assertError(Runnable operation, ErrorCode errorCode) {
    assertThatThrownBy(operation::run)
        .isInstanceOfSatisfying(
            CustomException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
  }

  private static class MutableClock extends Clock {
    private Instant instant;

    MutableClock(Instant instant) {
      this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
