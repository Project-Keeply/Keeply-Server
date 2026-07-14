package com.keeply.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StaffOnboardingRequest {

  @NotBlank(message = "이름은 필수입니다.")
  private String name;

  @NotBlank(message = "초대코드는 필수입니다.")
  @Size(min = 6, max = 6, message = "초대코드는 6자리여야 합니다.")
  @Pattern(regexp = "^[A-Z0-9]{6}$", message = "초대코드는 대문자 영숫자 6자리여야 합니다.")
  private String inviteCode;
}
