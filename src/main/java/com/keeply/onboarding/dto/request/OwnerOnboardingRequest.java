package com.keeply.onboarding.dto.request;

import com.keeply.group.entity.StoreBrand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OwnerOnboardingRequest {
  @NotBlank(message = "이름은 필수입니다.")
  private String name;

  @NotNull(message = "브랜드는 필수입니다.")
  private StoreBrand storeBrand;

  @NotBlank(message = "매장 이름은 필수입니다.")
  private String storeName;
}
