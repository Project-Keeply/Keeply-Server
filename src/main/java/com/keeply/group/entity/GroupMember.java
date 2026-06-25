package com.keeply.group.entity;

import com.keeply.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
    name = "group_members",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_group_members_user_id_active",
          columnNames = {"user_id", "deleted_flag"}),
      @UniqueConstraint(
          name = "uq_group_members_group_user",
          columnNames = {"group_id", "user_id"})
    })
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GroupMember {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private Group group;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GroupRole role;

  @CreatedDate
  @Column(name = "joined_at", nullable = false, updatable = false)
  private LocalDateTime joinedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  // 활성 멤버 1행 보장을 위한 파생 컬럼 (DB가 자동 계산, 애플리케이션에서 참조 X)
  @Column(
      name = "deleted_flag",
      insertable = false,
      updatable = false,
      columnDefinition = "TINYINT GENERATED ALWAYS AS (IF(deleted_at IS NULL, 0, NULL)) VIRTUAL")
  private Byte deletedFlag;

  public void markDeleted() {
    this.deletedAt = LocalDateTime.now();
  }

  public void reactivate(Group newGroup, GroupRole newRole) {
    this.group = newGroup;
    this.role = newRole;
    this.deletedAt = null;
  }
}
