package com.keeply.group.dto.request;

import com.keeply.group.entity.StoreBrand;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateGroupRequest {

  private String name;
  private StoreBrand storeBrand;
}
