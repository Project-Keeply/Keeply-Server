package com.keeply.group.dto.response;

import com.keeply.group.entity.Group;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.entity.StoreBrand;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupResponse {

  private final Long groupId;
  private final String groupName;
  private final StoreBrand storeBrand;
  private final String inviteCode;
  private final GroupRole role;

  public static GroupResponse of(Group group, GroupRole role) {
    return GroupResponse.builder()
        .groupId(group.getId())
        .groupName(group.getName())
        .storeBrand(group.getStoreBrand())
        .inviteCode(role == GroupRole.OWNER ? group.getInviteCode() : null)
        .role(role)
        .build();
  }
}
