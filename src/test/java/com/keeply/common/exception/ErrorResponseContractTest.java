package com.keeply.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keeply.auth.jwt.JwtAccessDeniedHandler;
import com.keeply.auth.jwt.JwtAuthenticationEntryPoint;
import com.keeply.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

class ErrorResponseContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ContractController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @ParameterizedTest
  @EnumSource(
      value = ErrorCode.class,
      names = {
        "REFRESH_TOKEN_MISSING",
        "REFRESH_TOKEN_EXPIRED",
        "REFRESH_TOKEN_INVALID",
        "REFRESH_TOKEN_MISMATCH",
        "AUTH_ORIGIN_NOT_ALLOWED",
        "INVALID_TOKEN",
        "INVALID_INPUT",
        "INTERNAL_SERVER_ERROR"
      })
  void customErrorsExposeStableCodeAndKeepMessageWithoutChangingCookies(ErrorCode errorCode)
      throws Exception {
    mockMvc
        .perform(get("/contract/error/{errorCode}", errorCode.name()))
        .andExpect(status().is(errorCode.getHttpStatus().value()))
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.message").value(errorCode.getMessage()))
        .andExpect(jsonPath("$.code").value(errorCode.getCode()))
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
  }

  @Test
  void successfulResponseOmitsCodeAndKeepsExistingNullFields() throws Exception {
    JsonNode response = objectMapper.valueToTree(ApiResponse.success(null));

    assertThat(response.get("success").asBoolean()).isTrue();
    assertThat(response.has("code")).isFalse();
    assertThat(response.has("data")).isTrue();
    assertThat(response.get("data").isNull()).isTrue();
    assertThat(response.has("message")).isTrue();
    assertThat(response.get("message").isNull()).isTrue();
  }

  @Test
  void legacyFailureHelperKeepsItsResponseShapeWithoutNullCode() {
    JsonNode response = objectMapper.valueToTree(ApiResponse.failure("기존 오류 메시지"));

    assertThat(response.get("success").asBoolean()).isFalse();
    assertThat(response.get("message").asText()).isEqualTo("기존 오류 메시지");
    assertThat(response.get("data").isNull()).isTrue();
    assertThat(response.has("code")).isFalse();
  }

  @Test
  void authenticationEntryPointExposesExistingAuthCodeWithoutCookieChange() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    new JwtAuthenticationEntryPoint(objectMapper)
        .commence(
            new MockHttpServletRequest(), response, new BadCredentialsException("invalid access"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(objectMapper.readTree(response.getContentAsString()).get("code").asText())
        .isEqualTo("AUTH_001");
    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
  }

  @Test
  void accessDeniedHandlerExposesExistingAuthCodeWithoutCookieChange() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    new JwtAccessDeniedHandler(objectMapper)
        .handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(objectMapper.readTree(response.getContentAsString()).get("code").asText())
        .isEqualTo("AUTH_003");
    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
  }

  @RestController
  static class ContractController {
    @GetMapping("/contract/error/{errorCode}")
    ApiResponse<Void> fail(@PathVariable ErrorCode errorCode) {
      throw new CustomException(errorCode);
    }
  }
}
