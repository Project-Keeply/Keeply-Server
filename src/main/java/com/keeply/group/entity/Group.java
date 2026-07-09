package com.keeply.group.entity;

import com.keeply.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "store_groups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Group extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String name;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "store_brand", nullable = false)
  private StoreBrand storeBrand;

  @NotBlank
  @Column(name = "invite_code", nullable = false, unique = true)
  private String inviteCode;

  public void updateInfo(String name, StoreBrand storeBrand) {
    if (name != null && !name.isBlank()) {
      this.name = name;
    }
    if (storeBrand != null) {
      this.storeBrand = storeBrand;
    }
  }

  public void updateInviteCode(String inviteCode) {
    this.inviteCode = inviteCode;
  }
}
