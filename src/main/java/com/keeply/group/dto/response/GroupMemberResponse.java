package com.keeply.group.dto.response;

import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupMemberResponse {
  private final Long userId;
  private final String name;
  private final String profileImageUrl;
  private final GroupRole role;
  private final LocalDateTime joinedAt;

  public static GroupMemberResponse of(GroupMember member) {
    return GroupMemberResponse.builder()
        .userId(member.getUser().getId())
        .name(member.getUser().getName())
        .profileImageUrl(member.getUser().getProfileImageUrl())
        .role(member.getRole())
        .joinedAt(member.getJoinedAt())
        .build();
  }
}
