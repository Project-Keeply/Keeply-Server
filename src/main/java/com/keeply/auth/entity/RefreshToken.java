package com.keeply.auth.entity;

import com.keeply.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken {

  private static final String SESSION_ID_PATTERN =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @NotBlank
  @Pattern(regexp = SESSION_ID_PATTERN)
  @Column(name = "session_id", nullable = false, length = 36)
  @Builder.Default
  private String sessionId = UUID.randomUUID().toString();

  @NotBlank
  @Column(name = "token", nullable = false, unique = true)
  private String tokenHash;

  @NotNull
  @Column(name = "expiry_date", nullable = false)
  private LocalDateTime expiryDate;

  public void updateToken(String tokenHash, LocalDateTime expiryDate) {
    this.tokenHash = tokenHash;
    this.expiryDate = expiryDate;
  }

  public void updateSession(String sessionId, String tokenHash, LocalDateTime expiryDate) {
    if (sessionId == null || !sessionId.matches(SESSION_ID_PATTERN)) {
      throw new IllegalArgumentException("세션 식별자는 UUID 형식이어야 합니다.");
    }
    this.sessionId = sessionId;
    updateToken(tokenHash, expiryDate);
  }
}
