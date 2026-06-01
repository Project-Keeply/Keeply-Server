package com.keeply.onboarding.dto.response;

import com.keeply.group.entity.Group;
import com.keeply.group.entity.StoreBrand;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OwnerOnboardingResponse {

  private final Long groupId;
  private final String groupName;
  private final StoreBrand storeBrand;
  private final String inviteCode;

  public static OwnerOnboardingResponse from(Group group) {
    return OwnerOnboardingResponse.builder()
        .groupId(group.getId())
        .groupName(group.getName())
        .storeBrand(group.getStoreBrand())
        .inviteCode(group.getInviteCode())
        .build();
  }
}
