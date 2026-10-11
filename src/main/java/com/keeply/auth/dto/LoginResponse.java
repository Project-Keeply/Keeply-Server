package com.keeply.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
  @NotBlank
  @Schema(description = "API 인증에 사용하는 Access Token", requiredMode = Schema.RequiredMode.REQUIRED)
  private String accessToken;
}
