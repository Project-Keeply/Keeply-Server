package com.keeply.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keeply.auth.cookie.RefreshTokenCookieProvider;
import com.keeply.auth.dto.TokenIssueResult;
import com.keeply.auth.entity.RefreshToken;
import com.keeply.auth.jwt.JwtProperties;
import com.keeply.auth.jwt.JwtProvider;
import com.keeply.auth.kakao.KakaoUserInfoResponse;
import com.keeply.auth.repository.RefreshTokenRepository;
import com.keeply.auth.service.AuthAccountService;
import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.group.entity.Group;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.entity.StoreBrand;
import com.keeply.group.repository.GroupMemberRepository;
import com.keeply.group.repository.GroupRepository;
import com.keeply.user.entity.User;
import com.keeply.user.repository.UserRepository;
import com.keeply.user.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** H2 기반 실제 HTTP/서비스/DB 검증이다. MySQL 잠금 동시성을 입증하지 않는다. */
@SpringBootTest(properties = "app.cors.allowed-origins[0]=http://localhost:5173")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/sql/auth-token-h2-group-members.sql")
class AuthTokenIntegrationTest {

  private static final String ORIGIN = "http://localhost:5173";
  private static final Long KAKAO_ID = 640012L;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthAccountService authAccountService;
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private GroupMemberRepository groupMemberRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private JwtProperties jwtProperties;
  @Autowired private Clock clock;

  @BeforeEach
  void clearFixtures() {
    refreshTokenRepository.deleteAllInBatch();
    groupRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }

  @Test
  void generatedOpenApiDescribesCookieOnlyPublicContract() throws Exception {
    MvcResult result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
    JsonNode document = objectMapper.readTree(result.getResponse().getContentAsString());
    JsonNode reissue = document.path("paths").path("/auth/reissue").path("post");
    assertThat(reissue.isMissingNode()).isFalse();
    assertThat(reissue.has("requestBody")).isFalse();
    assertThat(document.path("paths").has("/auth/refresh")).isFalse();
    assertThat(reissue.path("security").isMissingNode() || reissue.path("security").isEmpty())
        .isTrue();
    JsonNode logout = document.path("paths").path("/auth/logout").path("post");
    assertThat(logout.path("security").isMissingNode() || logout.path("security").isEmpty())
        .isTrue();
    assertThat(logout.has("requestBody")).isFalse();
    List<JsonNode> cookieParameters =
        StreamSupport.stream(reissue.path("parameters").spliterator(), false)
            .filter(
                parameter ->
                    RefreshTokenCookieProvider.NAME.equals(parameter.path("name").asText()))
            .filter(parameter -> "cookie".equals(parameter.path("in").asText()))
            .toList();
    assertThat(cookieParameters).hasSize(1);
    assertThat(cookieParameters.getFirst().path("required").asBoolean()).isFalse();
    JsonNode schemas = document.path("components").path("schemas");
    JsonNode login = schemas.path("LoginResponse");
    assertThat(login.path("properties").has("accessToken")).isTrue();
    assertThat(login.path("properties").has("refreshToken")).isFalse();
    assertThat(login.path("required").toString()).contains("accessToken");
    JsonNode error = schemas.path("ApiResponse");
    assertThat(error.path("properties").has("code")).isTrue();
    assertThat(error.path("required").toString()).doesNotContain("code");
  }

  @Test
  void actualRotationConsumesOldTokenAndKeepsSessionHashOnlyInDatabase() throws Exception {
    TokenIssueResult login = login();
    User user = userRepository.findByKakaoId(KAKAO_ID.toString()).orElseThrow();
    RefreshToken initial = refreshTokenRepository.findByUser(user).orElseThrow();
    assertThat(initial.getTokenHash()).isEqualTo(hashToken(login.refreshToken()));
    assertThat(initial.getTokenHash()).doesNotContain(login.refreshToken());
    String sessionId = initial.getSessionId();

    MvcResult result = reissue(login.refreshToken(), expiredAccessToken(user.getId()));
    String rotated = getRefreshToken(result);
    assertThat(rotated).isNotEqualTo(login.refreshToken());
    assertThat(jwtProvider.parseRefreshToken(rotated).sessionId()).isEqualTo(sessionId);
    RefreshToken saved = refreshTokenRepository.findByUser(user).orElseThrow();
    assertThat(saved.getSessionId()).isEqualTo(sessionId);
    assertThat(saved.getTokenHash()).isEqualTo(hashToken(rotated));

    assertInvalidReissue(login.refreshToken(), "REFRESH_TOKEN_MISMATCH");
    reissue(rotated, null);
  }

  @Test
  void oldTokenFromSameSessionCanLogoutAfterRotationAndCannotResurrectSession() throws Exception {
    TokenIssueResult login = login();
    String rotated = getRefreshToken(reissue(login.refreshToken(), null));

    mockMvc
        .perform(
            post("/auth/logout")
                .header(HttpHeaders.ORIGIN, ORIGIN)
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-access")
                .cookie(cookie(login.refreshToken())))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

    assertThat(refreshTokenRepository.count()).isZero();
    assertInvalidReissue(rotated, "REFRESH_TOKEN_INVALID");
    assertThat(refreshTokenRepository.count()).isZero();
  }

  @Test
  void oldLoginSessionCannotLogoutOrRotateLatestLoginSession() throws Exception {
    TokenIssueResult oldLogin = login();
    TokenIssueResult latestLogin = login();
    assertThat(jwtProvider.parseRefreshToken(oldLogin.refreshToken()).sessionId())
        .isNotEqualTo(jwtProvider.parseRefreshToken(latestLogin.refreshToken()).sessionId());

    mockMvc
        .perform(
            post("/auth/logout")
                .header(HttpHeaders.ORIGIN, ORIGIN)
                .cookie(cookie(oldLogin.refreshToken())))
        .andExpect(status().isOk());
    assertInvalidReissue(oldLogin.refreshToken(), "REFRESH_TOKEN_INVALID");
    reissue(latestLogin.refreshToken(), null);
  }

  @Test
  void withdrawalDeletesSessionAndRejectsReissue() throws Exception {
    TokenIssueResult login = login();
    Long userId = jwtProvider.parseRefreshToken(login.refreshToken()).userId();

    userService.delete(userId);

    assertThat(userRepository.findById(userId).orElseThrow().isDeleted()).isTrue();
    assertThat(refreshTokenRepository.count()).isZero();
    assertInvalidReissue(login.refreshToken(), "REFRESH_TOKEN_INVALID");
  }

  @Test
  void staffWithdrawalDeletesMembershipAndSessionInRealTransaction() throws Exception {
    TokenIssueResult login = login();
    Long userId = jwtProvider.parseRefreshToken(login.refreshToken()).userId();
    createMembership(userId, GroupRole.STAFF);

    userService.delete(userId);

    assertThat(groupMemberRepository.findByUserId(userId)).isEmpty();
    assertThat(userRepository.findById(userId).orElseThrow().isDeleted()).isTrue();
    assertThat(refreshTokenRepository.count()).isZero();
    assertInvalidReissue(login.refreshToken(), "REFRESH_TOKEN_INVALID");
  }

  @Test
  void ownerWithdrawalRollsBackAndKeepsActiveMembershipAndSession() throws Exception {
    TokenIssueResult login = login();
    Long userId = jwtProvider.parseRefreshToken(login.refreshToken()).userId();
    createMembership(userId, GroupRole.OWNER);

    assertThatThrownBy(() -> userService.delete(userId))
        .isInstanceOfSatisfying(
            CustomException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OWNER_CANNOT_WITHDRAW));

    assertThat(userRepository.findById(userId).orElseThrow().isDeleted()).isFalse();
    assertThat(groupMemberRepository.findByUserId(userId)).isPresent();
    reissue(login.refreshToken(), null);
  }

  private void createMembership(Long userId, GroupRole role) {
    Group group =
        groupRepository.saveAndFlush(
            Group.builder().name("인증 검증점").storeBrand(StoreBrand.CU).inviteCode("AUTH64").build());
    groupMemberRepository.saveAndFlush(
        GroupMember.builder()
            .group(group)
            .user(userRepository.findById(userId).orElseThrow())
            .role(role)
            .build());
  }

  @Test
  void missingCookieIsUnauthorizedButLogoutWithoutCookieIsIdempotent() throws Exception {
    mockMvc
        .perform(post("/auth/reissue").header(HttpHeaders.ORIGIN, ORIGIN))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_MISSING"))
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    mockMvc
        .perform(post("/auth/logout").header(HttpHeaders.ORIGIN, ORIGIN))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
  }

  private TokenIssueResult login() throws Exception {
    return authAccountService.processLogin(
        objectMapper.readValue(
            "{\"id\":" + KAKAO_ID + ",\"kakao_account\":{\"profile\":{\"nickname\":\"통합 검증\"}}}",
            KakaoUserInfoResponse.class));
  }

  private MvcResult reissue(String token, String authorization) throws Exception {
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
        post("/auth/reissue").header(HttpHeaders.ORIGIN, ORIGIN).cookie(cookie(token));
    if (authorization != null) {
      request.header(HttpHeaders.AUTHORIZATION, authorization);
    }
    return mockMvc
        .perform(request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
        .andExpect(
            header()
                .string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
        .andExpect(
            header()
                .string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Path=/auth")))
        .andReturn();
  }

  private void assertInvalidReissue(String token, String code) throws Exception {
    mockMvc
        .perform(post("/auth/reissue").header(HttpHeaders.ORIGIN, ORIGIN).cookie(cookie(token)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
  }

  private String getRefreshToken(MvcResult result) {
    String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    assertThat(header).startsWith(RefreshTokenCookieProvider.NAME + "=");
    return header.substring(header.indexOf('=') + 1, header.indexOf(';'));
  }

  private Cookie cookie(String token) {
    return new Cookie(RefreshTokenCookieProvider.NAME, token);
  }

  private String expiredAccessToken(Long userId) {
    return "Bearer "
        + Jwts.builder()
            .subject(userId.toString())
            .claim("type", "access")
            .issuedAt(Date.from(clock.instant().minusSeconds(60)))
            .expiration(Date.from(clock.instant().minusSeconds(1)))
            .signWith(
                Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)))
            .compact();
  }

  private String hashToken(String token) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
  }
}
