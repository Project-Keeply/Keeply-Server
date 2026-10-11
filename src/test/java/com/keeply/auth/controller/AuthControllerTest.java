package com.keeply.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keeply.auth.cookie.RefreshTokenCookieProperties;
import com.keeply.auth.cookie.RefreshTokenCookieProvider;
import com.keeply.auth.dto.TokenIssueResult;
import com.keeply.auth.service.AuthService;
import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.common.exception.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Controller 계약 검증. 실제 Security 필터/Origin 검증은 별도의 통합 테스트에서 확인한다. */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");

  @Mock private AuthService authService;

  private MockMvc mockMvc;
  private AuthController authController;

  @BeforeEach
  void setUp() {
    RefreshTokenCookieProperties properties = new RefreshTokenCookieProperties();
    properties.setSecure(true);
    properties.setSameSite("None");
    RefreshTokenCookieProvider cookieProvider =
        new RefreshTokenCookieProvider(properties, Clock.fixed(NOW, ZoneOffset.UTC));
    authController = new AuthController(authService, cookieProvider);
    mockMvc =
        MockMvcBuilders.standaloneSetup(authController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void loginReturnsOnlyAccessTokenAndIssuesCookieWithoutOverwritingOtherCookies() throws Exception {
    when(authService.login("kakao-code")).thenReturn(issueResult());
    mockMvc =
        MockMvcBuilders.standaloneSetup(authController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(
                (request, response, chain) -> {
                  ((jakarta.servlet.http.HttpServletResponse) response)
                      .addHeader(HttpHeaders.SET_COOKIE, "other=value; Path=/");
                  chain.doFilter(request, response);
                })
            .build();

    MvcResult result =
        mockMvc
            .perform(
                post("/auth/kakao/callback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"kakao-code\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("new-access"))
            .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
            .andReturn();

    assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
        .hasSize(2)
        .contains("other=value; Path=/");
    assertIssuedCookie(result);
    verify(authService).login("kakao-code");
  }

  @Test
  void rejectsBlankKakaoCodeBeforeIssuingCookie() throws Exception {
    mockMvc
        .perform(
            post("/auth/kakao/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

    verifyNoInteractions(authService);
  }

  @Test
  void reissuesFromCookieWithoutBodyAndReplacesRefreshCookie() throws Exception {
    when(authService.reissue("old-refresh")).thenReturn(issueResult());

    MvcResult result =
        mockMvc
            .perform(post("/auth/reissue").cookie(refreshCookie("old-refresh")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("new-access"))
            .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
            .andReturn();

    assertIssuedCookie(result);
    verify(authService).reissue("old-refresh");
  }

  @Test
  void missingReissueCookieProducesUnauthorizedWithoutCookieChange() throws Exception {
    when(authService.reissue(null)).thenThrow(new CustomException(ErrorCode.REFRESH_TOKEN_MISSING));

    mockMvc
        .perform(post("/auth/reissue"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

    verify(authService).reissue(null);
  }

  @ParameterizedTest
  @EnumSource(
      value = ErrorCode.class,
      names = {"REFRESH_TOKEN_EXPIRED", "REFRESH_TOKEN_INVALID", "REFRESH_TOKEN_MISMATCH"})
  void rejectedReissueNeverChangesCookie(ErrorCode errorCode) throws Exception {
    when(authService.reissue("rejected-refresh")).thenThrow(new CustomException(errorCode));

    mockMvc
        .perform(post("/auth/reissue").cookie(refreshCookie("rejected-refresh")))
        .andExpect(status().isUnauthorized())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
  }

  @Test
  void reissueServerFailureNeverChangesCookie() throws Exception {
    when(authService.reissue("old-refresh"))
        .thenThrow(new IllegalStateException("test transaction failure"));

    mockMvc
        .perform(post("/auth/reissue").cookie(refreshCookie("old-refresh")))
        .andExpect(status().isInternalServerError())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
  }

  @Test
  void logoutWithoutCookieIsIdempotentAndClearsCookie() throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isEmpty())
            .andReturn();

    assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
        .startsWith("keeply_refresh_token=;")
        .contains("Path=/auth", "Max-Age=0", "Secure", "HttpOnly", "SameSite=None")
        .doesNotContain("Domain=");
    verify(authService).logoutSession(null);
  }

  @Test
  void logoutUsesCookieDespiteExpiredAuthorizationHeader() throws Exception {
    mockMvc
        .perform(
            post("/auth/logout")
                .cookie(refreshCookie("session-refresh"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-access"))
        .andExpect(status().isOk())
        .andExpect(header().exists(HttpHeaders.SET_COOKIE));

    verify(authService).logoutSession("session-refresh");
  }

  @Test
  void failedLogoutNeverClearsCookie() throws Exception {
    doThrow(new IllegalStateException("test transaction failure"))
        .when(authService)
        .logoutSession("session-refresh");

    mockMvc
        .perform(post("/auth/logout").cookie(refreshCookie("session-refresh")))
        .andExpect(status().isInternalServerError())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
  }

  @Test
  void legacyJsonRefreshEndpointIsRemoved() throws Exception {
    // 경로 매핑만 검증한다. 공통 예외 처리기의 미등록 경로 정책은 별도 범위다.
    mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    mockMvc
        .perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"old-refresh\"}"))
        .andExpect(status().isNotFound())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

    verifyNoInteractions(authService);
  }

  private TokenIssueResult issueResult() {
    return new TokenIssueResult("new-access", "new-refresh", NOW.plusSeconds(120));
  }

  private Cookie refreshCookie(String token) {
    return new Cookie(RefreshTokenCookieProvider.NAME, token);
  }

  private void assertIssuedCookie(MvcResult result) {
    assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
        .anySatisfy(
            cookie ->
                assertThat(cookie)
                    .startsWith("keeply_refresh_token=new-refresh;")
                    .contains("Path=/auth", "Max-Age=120", "Secure", "HttpOnly", "SameSite=None")
                    .doesNotContain("Domain="));
  }
}
