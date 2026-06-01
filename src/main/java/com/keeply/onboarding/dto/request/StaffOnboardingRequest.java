package com.keeply.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StaffOnboardingRequest {

  @NotBlank(message = "이름은 필수입니다.")
  private String name;

  @NotBlank(message = "초대코드는 필수입니다.")
  private String inviteCode;
}
