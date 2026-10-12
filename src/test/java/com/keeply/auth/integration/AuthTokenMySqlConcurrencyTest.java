package com.keeply.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keeply.auth.cookie.RefreshTokenCookieProvider;
import com.keeply.auth.dto.TokenIssueResult;
import com.keeply.auth.jwt.JwtProvider;
import com.keeply.auth.kakao.KakaoUserInfoResponse;
import com.keeply.auth.repository.RefreshTokenRepository;
import com.keeply.auth.service.AuthAccountService;
import com.keeply.user.repository.UserRepository;
import com.keeply.user.service.UserService;
import jakarta.servlet.http.Cookie;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 실제 MySQL의 독립 트랜잭션 경합. 테스트 전용 로컬 DB에서만 create-drop을 허용한다. */
@SpringBootTest(properties = "app.cors.allowed-origins[0]=http://localhost:5173")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "KEEPLY_AUTH_MYSQL_TEST_URL", matches = ".+")
class AuthTokenMySqlConcurrencyTest {
  private static final String ORIGIN = "http://localhost:5173";
  private static final Long KAKAO_ID = 640099L;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthAccountService authAccountService;
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private JwtProvider jwtProvider;

  @DynamicPropertySource
  static void configureDedicatedMySql(DynamicPropertyRegistry registry) throws SQLException {
    // 모든 검증을 먼저 끝낸 뒤 datasource를 등록하므로 잘못된 URL로 연결/DDL을 실행하지 않는다.
    String url =
        MySqlTestDatabaseGuard.requireDedicatedDatabaseUrl(
            System.getenv("KEEPLY_AUTH_MYSQL_TEST_URL"));
    String username =
        MySqlTestDatabaseGuard.requireCredential(System.getenv("KEEPLY_AUTH_MYSQL_TEST_USERNAME"));
    String password =
        MySqlTestDatabaseGuard.requireCredential(System.getenv("KEEPLY_AUTH_MYSQL_TEST_PASSWORD"));
    Properties connectionProperties = new Properties();
    connectionProperties.setProperty("user", username);
    connectionProperties.setProperty("password", password);
    connectionProperties.setProperty("connectTimeout", "3000");
    connectionProperties.setProperty("socketTimeout", "3000");
    try (Connection connection = DriverManager.getConnection(url, connectionProperties)) {
      connection.setReadOnly(true);
      if (!"MySQL".equals(connection.getMetaData().getDatabaseProductName())) {
        throw new IllegalArgumentException("동시성 테스트는 실제 MySQL 서버만 허용합니다.");
      }
      MySqlTestDatabaseGuard.requireMySql8Version(
          connection.getMetaData().getDatabaseProductVersion());
    }
    registry.add("spring.datasource.url", () -> url);
    registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    registry.add("spring.datasource.username", () -> username);
    registry.add("spring.datasource.password", () -> password);
    registry.add("spring.datasource.hikari.maximum-pool-size", () -> 4);
    registry.add("spring.datasource.hikari.connection-timeout", () -> 5000);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    registry.add("spring.flyway.enabled", () -> false);
  }

  @BeforeEach
  void clearFixtures() {
    refreshTokenRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }

  @Test
  void concurrentRefreshConsumesTokenOnceAndWinnerCanRefreshAgain() throws Exception {
    TokenIssueResult login = login();

    List<HttpResult> results =
        runConcurrently(() -> reissue(login.refreshToken()), () -> reissue(login.refreshToken()));

    assertThat(results.stream().map(HttpResult::status).sorted().toList())
        .containsExactly(200, 401);
    HttpResult failure =
        results.stream().filter(result -> result.status() == 401).findFirst().orElseThrow();
    assertThat(failure.code()).isEqualTo("REFRESH_TOKEN_MISMATCH");
    assertThat(failure.setCookie()).isNull();
    HttpResult winner =
        results.stream().filter(result -> result.status() == 200).findFirst().orElseThrow();
    String rotated = getRefreshToken(winner);
    assertThat(jwtProvider.parseRefreshToken(rotated).sessionId())
        .isEqualTo(jwtProvider.parseRefreshToken(login.refreshToken()).sessionId());
    assertThat(reissue(rotated).status()).isEqualTo(200);
  }

  @Test
  void concurrentLogoutCannotLeaveOrResurrectRefreshSession() throws Exception {
    TokenIssueResult login = login();

    List<HttpResult> results =
        runConcurrently(() -> reissue(login.refreshToken()), () -> logout(login.refreshToken()));

    assertRefreshSucceededOrSessionWasRevoked(results.getFirst());
    assertThat(results.getLast().status()).isEqualTo(200);
    assertThat(refreshTokenRepository.count()).isZero();
    String candidate =
        results.getFirst().status() == 200
            ? getRefreshToken(results.getFirst())
            : login.refreshToken();
    assertThat(reissue(candidate).code()).isEqualTo("REFRESH_TOKEN_INVALID");
    assertThat(refreshTokenRepository.count()).isZero();
  }

  @Test
  void concurrentLoginReplacesSessionWithoutOldSessionLogoutDeletingNewOne() throws Exception {
    TokenIssueResult firstLogin = login();
    String oldSessionId = jwtProvider.parseRefreshToken(firstLogin.refreshToken()).sessionId();

    List<HttpResult> results =
        runConcurrently(
            () -> reissue(firstLogin.refreshToken()),
            () -> {
              TokenIssueResult latestLogin = login();
              return new HttpResult(
                  200,
                  null,
                  RefreshTokenCookieProvider.NAME + "=" + latestLogin.refreshToken() + ";");
            });

    assertRefreshSucceededOrSessionWasRevoked(results.getFirst());
    String latest = getRefreshToken(results.getLast());
    assertThat(jwtProvider.parseRefreshToken(latest).sessionId()).isNotEqualTo(oldSessionId);
    assertThat(logout(firstLogin.refreshToken()).status()).isEqualTo(200);
    assertThat(reissue(latest).status()).isEqualTo(200);
  }

  @Test
  void concurrentWithdrawalLeavesDeletedUserWithoutRefreshSession() throws Exception {
    TokenIssueResult login = login();
    Long userId = jwtProvider.parseRefreshToken(login.refreshToken()).userId();

    List<HttpResult> results =
        runConcurrently(
            () -> reissue(login.refreshToken()),
            () -> {
              userService.delete(userId);
              return new HttpResult(200, null, null);
            });

    assertRefreshSucceededOrSessionWasRevoked(results.getFirst());
    assertThat(userRepository.findById(userId).orElseThrow().isDeleted()).isTrue();
    assertThat(refreshTokenRepository.count()).isZero();
    String candidate =
        results.getFirst().status() == 200
            ? getRefreshToken(results.getFirst())
            : login.refreshToken();
    assertThat(reissue(candidate).code()).isEqualTo("REFRESH_TOKEN_INVALID");
    assertThat(refreshTokenRepository.count()).isZero();
  }

  private List<HttpResult> runConcurrently(Callable<HttpResult> first, Callable<HttpResult> second)
      throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CyclicBarrier barrier = new CyclicBarrier(2);
    try {
      Future<HttpResult> firstFuture =
          executor.submit(
              () -> {
                barrier.await(5, TimeUnit.SECONDS);
                return first.call();
              });
      Future<HttpResult> secondFuture =
          executor.submit(
              () -> {
                barrier.await(5, TimeUnit.SECONDS);
                return second.call();
              });
      return List.of(firstFuture.get(20, TimeUnit.SECONDS), secondFuture.get(20, TimeUnit.SECONDS));
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  private TokenIssueResult login() throws Exception {
    return authAccountService.processLogin(
        objectMapper.readValue(
            "{\"id\":"
                + KAKAO_ID
                + ",\"kakao_account\":{\"profile\":{\"nickname\":\"MySQL 통합 검증\"}}}",
            KakaoUserInfoResponse.class));
  }

  private HttpResult reissue(String token) throws Exception {
    return callEndpoint("/auth/reissue", token);
  }

  private HttpResult logout(String token) throws Exception {
    return callEndpoint("/auth/logout", token);
  }

  private HttpResult callEndpoint(String path, String token) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post(path)
                    .header(HttpHeaders.ORIGIN, ORIGIN)
                    .cookie(new Cookie(RefreshTokenCookieProvider.NAME, token)))
            .andReturn();
    return new HttpResult(
        result.getResponse().getStatus(),
        objectMapper.readTree(result.getResponse().getContentAsString()).path("code").asText(null),
        result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
  }

  private String getRefreshToken(HttpResult result) {
    assertThat(result.setCookie()).startsWith(RefreshTokenCookieProvider.NAME + "=");
    return result
        .setCookie()
        .substring(result.setCookie().indexOf('=') + 1, result.setCookie().indexOf(';'));
  }

  private void assertRefreshSucceededOrSessionWasRevoked(HttpResult result) {
    assertThat(result.status()).isIn(200, 401);
    if (result.status() == 401) {
      assertThat(result.code()).isEqualTo("REFRESH_TOKEN_INVALID");
      assertThat(result.setCookie()).isNull();
    }
  }

  private record HttpResult(int status, String code, String setCookie) {}
}
