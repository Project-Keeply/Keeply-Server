package com.keeply.auth.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keeply.auth.cookie.RefreshTokenCookieProperties;
import com.keeply.auth.cookie.RefreshTokenCookieProvider;
import com.keeply.auth.dto.TokenIssueResult;
import com.keeply.auth.jwt.JwtAccessDeniedHandler;
import com.keeply.auth.jwt.JwtAuthenticationEntryPoint;
import com.keeply.auth.jwt.JwtAuthenticationFilter;
import com.keeply.auth.jwt.JwtProvider;
import com.keeply.auth.service.AuthService;
import com.keeply.common.config.SecurityConfig;
import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 실제 SecurityFilterChain, CORS, JWT 필터와 Controller를 함께 검증한다. DB는 사용하지 않는다. */
@WebMvcTest(
    controllers = AuthController.class,
    properties = "app.cors.allowed-origins[0]=http://localhost:5173")
@ActiveProfiles("test")
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  JwtAuthenticationEntryPoint.class,
  JwtAccessDeniedHandler.class,
  RefreshTokenCookieProvider.class,
  RefreshTokenCookieProperties.class,
  AuthenticationSecurityTest.TimeConfig.class
})
class AuthenticationSecurityTest {

  private static final String ORIGIN = "http://localhost:5173";
  private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AuthService authService;
  @MockitoBean private JwtProvider jwtProvider;
  @MockitoBean private UserRepository userRepository;
  @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

  @ParameterizedTest
  @ValueSource(strings = {"/auth/kakao/callback", "/auth/reissue", "/auth/logout"})
  void rejectsMissingOriginBeforeInvokingAuthenticationService(String path) throws Exception {
    assertOriginRejected(post(path));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"null", "https://attacker.test", "http://localhost:5173.attacker.test", ""})
  void rejectsOriginsWithoutExactAllowlistMatch(String origin) throws Exception {
    assertOriginRejected(post("/auth/reissue").header(HttpHeaders.ORIGIN, origin));
  }

  @Test
  void rejectsDuplicateAndCommaJoinedOriginHeaders() throws Exception {
    assertOriginRejected(
        post("/auth/reissue").header(HttpHeaders.ORIGIN, ORIGIN, "https://attacker.test"));
    assertOriginRejected(
        post("/auth/reissue").header(HttpHeaders.ORIGIN, ORIGIN + ", https://attacker.test"));
  }

  @Test
  void permittedOriginMissingCookieReachesServiceAndReturnsUnauthorizedWithCors() throws Exception {
    when(authService.reissue(null)).thenThrow(new CustomException(ErrorCode.REFRESH_TOKEN_MISSING));

    mockMvc
        .perform(post("/auth/reissue").header(HttpHeaders.ORIGIN, ORIGIN))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

    verify(authService).reissue(null);
    verifyNoInteractions(jwtProvider, userRepository);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"Bearer expired-access", "Bearer invalid-access", "Bearer ", "unusual-header"})
  void reissueUsesCookieWithoutParsingAccessAuthorization(String authorization) throws Exception {
    when(authService.reissue("refresh-cookie")).thenReturn(issueResult());

    mockMvc
        .perform(
            post("/auth/reissue")
                .header(HttpHeaders.ORIGIN, ORIGIN)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .cookie(refreshCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").value("new-access"))
        .andExpect(header().exists(HttpHeaders.SET_COOKIE));

    verify(authService).reissue("refresh-cookie");
    verifyNoInteractions(jwtProvider, userRepository);
  }

  @Test
  void loginAlsoIgnoresAccessHeaderAndKeepsRefreshTokenOutOfJson() throws Exception {
    when(authService.login("kakao-code")).thenReturn(issueResult());

    mockMvc
        .perform(
            post("/auth/kakao/callback")
                .header(HttpHeaders.ORIGIN, ORIGIN)
                .header(HttpHeaders.AUTHORIZATION, "Bearer malformed-access")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"kakao-code\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
        .andExpect(header().exists(HttpHeaders.SET_COOKIE));

    verify(authService).login("kakao-code");
    verifyNoInteractions(jwtProvider, userRepository);
  }

  @Test
  void logoutIgnoresExpiredAccessHeaderAndExpiresRefreshCookie() throws Exception {
    mockMvc
        .perform(
            post("/auth/logout")
                .header(HttpHeaders.ORIGIN, ORIGIN)
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-access")
                .cookie(refreshCookie()))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

    verify(authService).logoutSession("refresh-cookie");
    verifyNoInteractions(jwtProvider, userRepository);
  }

  @Test
  void logoutWithoutCookieOrAccessTokenIsPermitted() throws Exception {
    mockMvc
        .perform(post("/auth/logout").header(HttpHeaders.ORIGIN, ORIGIN))
        .andExpect(status().isOk());

    verify(authService).logoutSession(null);
    verifyNoInteractions(jwtProvider, userRepository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/auth/kakao/callback", "/auth/reissue", "/auth/logout"})
  void preflightIsHandledByCorsWithoutCookieOrJwt(String path) throws Exception {
    mockMvc
        .perform(
            options(path)
                .header(HttpHeaders.ORIGIN, ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

    verifyNoInteractions(authService, jwtProvider, userRepository);
  }

  @Test
  void unconfiguredPreflightOriginIsRejectedByCors() throws Exception {
    mockMvc
        .perform(
            options("/auth/reissue")
                .header(HttpHeaders.ORIGIN, "https://attacker.test")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

    verifyNoInteractions(authService, jwtProvider, userRepository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/auth/kakao/callback", "/auth/reissue", "/auth/logout", "/users/me"})
  void getRequestsAreNotMadePublicByCookieAuthenticationMatchers(String path) throws Exception {
    mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    verifyNoInteractions(authService);
  }

  @Test
  void protectedRequestsStillUseJwtAuthenticationAndRejectInvalidBearer() throws Exception {
    when(jwtProvider.validateToken("invalid-access")).thenReturn(false);

    mockMvc
        .perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-access"))
        .andExpect(status().isUnauthorized());

    verify(jwtProvider).validateToken("invalid-access");
    verifyNoInteractions(authService, userRepository);
  }

  @Test
  void legacyRefreshEndpointIsNoLongerPublic() throws Exception {
    mockMvc
        .perform(post("/auth/refresh").header(HttpHeaders.ORIGIN, ORIGIN))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(authService);
  }

  private void assertOriginRejected(MockHttpServletRequestBuilder request) throws Exception {
    mockMvc
        .perform(request)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value(ErrorCode.AUTH_ORIGIN_NOT_ALLOWED.getMessage()))
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    verifyNoInteractions(authService, jwtProvider, userRepository);
  }

  private TokenIssueResult issueResult() {
    return new TokenIssueResult("new-access", "new-refresh", NOW.plusSeconds(120));
  }

  private Cookie refreshCookie() {
    return new Cookie(RefreshTokenCookieProvider.NAME, "refresh-cookie");
  }

  @TestConfiguration
  static class TimeConfig {
    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
