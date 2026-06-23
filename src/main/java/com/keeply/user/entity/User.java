package com.keeply.user.entity;

import com.keeply.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(name = "kakao_id", unique = true, nullable = false)
  private String kakaoId;

  @NotBlank
  @Column(nullable = false)
  private String name;

  @Column(name = "profile_image_url")
  private String profileImageUrl;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "is_name_customized", nullable = false)
  private boolean isNameCustomized;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void updateName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("이름은 null 또는 공백일 수 없습니다.");
    }
    this.name = name;
    this.isNameCustomized = true;
  }

  public void syncFromKakao(String kakaoNickname, String kakaoProfileImageUrl) {
    if (!isNameCustomized && kakaoNickname != null && !kakaoNickname.isBlank()) {
      this.name = kakaoNickname;
    }
    this.profileImageUrl = kakaoProfileImageUrl;
  }

  public void markDeleted() {
    this.deletedAt = LocalDateTime.now();
  }

  public boolean isDeleted() {
    return this.deletedAt != null;
  }
}
