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
public class StaffOnboardingResponse {

  private final Long groupId;
  private final String groupName;
  private final StoreBrand storeBrand;

  public static StaffOnboardingResponse from(Group group) {
    return StaffOnboardingResponse.builder()
        .groupId(group.getId())
        .groupName(group.getName())
        .storeBrand(group.getStoreBrand())
        .build();
  }
}
