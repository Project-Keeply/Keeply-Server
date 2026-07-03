package com.keeply.worklog.entity;

import com.keeply.common.entity.BaseTimeEntity;
import com.keeply.group.entity.Group;
import com.keeply.group.entity.GroupMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "work_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkLog extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumns({
    @JoinColumn(name = "group_id", referencedColumnName = "group_id", nullable = false),
    @JoinColumn(name = "author_user_id", referencedColumnName = "user_id", nullable = false)
  })
  private GroupMember authorMember;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", insertable = false, updatable = false)
  private Group group;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  public void updateContent(String content) {
    if (content != null && !content.isBlank()) {
      this.content = content;
    }
  }

  public boolean isAuthor(Long userId) {
    return authorMember.getUser().getId().equals(userId);
  }
}
