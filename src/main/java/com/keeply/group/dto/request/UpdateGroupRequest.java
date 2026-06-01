package com.keeply.group.dto.request;

import com.keeply.group.entity.StoreBrand;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateGroupRequest {

  private String name;
  private StoreBrand storeBrand;

  @AssertTrue(message = "수정할 항목이 최소 하나 이상 있어야 합니다.")
  public boolean isAtLeastOneFieldProvided() {
    return (name != null && !name.isBlank()) || storeBrand != null;
  }
}
