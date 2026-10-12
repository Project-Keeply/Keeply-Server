package com.keeply.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keeply.common.response.ApiResponse;
import org.junit.jupiter.api.Test;

class LoginResponseTest {
  @Test
  void apiResponseContainsOnlyAccessToken() {
    LoginResponse response = LoginResponse.builder().accessToken("access-token").build();

    JsonNode json = new ObjectMapper().valueToTree(ApiResponse.success(response));

    assertThat(json.path("success").asBoolean()).isTrue();
    assertThat(json.path("data").path("accessToken").asText()).isEqualTo("access-token");
    assertThat(json.path("data").size()).isEqualTo(1);
    assertThat(json.path("data").has("refreshToken")).isFalse();
    assertThat(json.path("data").has("refreshTokenExpiresAt")).isFalse();
  }
}
