package com.keeply.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keeply.user.entity.User;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RefreshTokenTest {

  private static final String SESSION_ID = "72a49752-657f-424b-8e34-43803024cda1";
  private static final String NEW_SESSION_ID = "630bff91-3e42-4b06-8d49-32486606aa10";
  private static final LocalDateTime EXPIRY_DATE = LocalDateTime.of(2026, 10, 25, 12, 0);

  @Test
  void updateTokenPreservesLoginSession() {
    RefreshToken refreshToken = createRefreshToken(SESSION_ID);
    LocalDateTime newExpiryDate = EXPIRY_DATE.plusDays(1);

    refreshToken.updateToken("rotated-token-hash", newExpiryDate);

    assertThat(refreshToken.getSessionId()).isEqualTo(SESSION_ID);
    assertThat(refreshToken.getTokenHash()).isEqualTo("rotated-token-hash");
    assertThat(refreshToken.getExpiryDate()).isEqualTo(newExpiryDate);
  }

  @Test
  void updateSessionReplacesLoginSessionAndToken() {
    RefreshToken refreshToken = createRefreshToken(SESSION_ID);
    LocalDateTime newExpiryDate = EXPIRY_DATE.plusDays(1);

    refreshToken.updateSession(NEW_SESSION_ID, "new-login-token-hash", newExpiryDate);

    assertThat(refreshToken.getSessionId()).isEqualTo(NEW_SESSION_ID);
    assertThat(refreshToken.getTokenHash()).isEqualTo("new-login-token-hash");
    assertThat(refreshToken.getExpiryDate()).isEqualTo(newExpiryDate);
  }

  @Test
  void builderGeneratesUuidWhenSessionIdIsOmitted() {
    RefreshToken refreshToken = RefreshToken.builder().build();

    assertThat(UUID.fromString(refreshToken.getSessionId()).toString())
        .isEqualTo(refreshToken.getSessionId());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "invalid-session-id", "1-1-1-1-1"})
  void updateSessionRejectsInvalidIdWithoutChangingExistingSession(String sessionId) {
    RefreshToken refreshToken = createRefreshToken(SESSION_ID);

    assertThatThrownBy(
            () -> refreshToken.updateSession(sessionId, "new-token-hash", EXPIRY_DATE.plusDays(1)))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(refreshToken.getSessionId()).isEqualTo(SESSION_ID);
    assertThat(refreshToken.getTokenHash()).isEqualTo("original-token-hash");
    assertThat(refreshToken.getExpiryDate()).isEqualTo(EXPIRY_DATE);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "invalid-session-id", "1-1-1-1-1"})
  void validationRejectsInvalidSessionIdProvidedByBuilder(String sessionId) {
    RefreshToken refreshToken = createRefreshToken(sessionId);

    try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      assertThat(validatorFactory.getValidator().validateProperty(refreshToken, "sessionId"))
          .isNotEmpty();
    }
  }

  private RefreshToken createRefreshToken(String sessionId) {
    return RefreshToken.builder()
        .user(User.builder().id(1L).build())
        .sessionId(sessionId)
        .tokenHash("original-token-hash")
        .expiryDate(EXPIRY_DATE)
        .build();
  }
}
