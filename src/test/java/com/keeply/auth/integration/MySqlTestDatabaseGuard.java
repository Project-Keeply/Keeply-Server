package com.keeply.auth.integration;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** create-drop 실행 전 전용 로컬 DB만 허용한다. URL이나 자격 증명을 오류 메시지에 출력하지 않는다. */
final class MySqlTestDatabaseGuard {
  private static final Pattern TEST_URL =
      Pattern.compile(
          "^jdbc:mysql://(?:localhost|127\\.0\\.0\\.1):([1-9][0-9]{0,4})/keeply_auth_test_[A-Za-z0-9_]{1,40}$");

  private MySqlTestDatabaseGuard() {}

  static String requireDedicatedDatabaseUrl(String url) {
    if (url == null) {
      throw new IllegalArgumentException("MySQL 전용 테스트 DB URL이 필요합니다.");
    }
    Matcher matcher = TEST_URL.matcher(url);
    if (!matcher.matches() || Integer.parseInt(matcher.group(1)) > 65535) {
      throw new IllegalArgumentException("MySQL 테스트는 loopback의 keeply_auth_test_* 전용 DB만 허용합니다.");
    }
    return url;
  }

  static String requireMySql8Version(String version) {
    if (version == null
        || !version.matches("8\\.[0-9]+\\.[0-9]+(?:[-+ ].*)?")
        || version.toLowerCase(Locale.ROOT).contains("mariadb")) {
      throw new IllegalArgumentException("동시성 테스트는 실제 MySQL 8.x 서버만 허용합니다.");
    }
    return version;
  }

  static String requireCredential(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("MySQL 테스트 DB 계정과 비밀번호를 환경 변수로 명시해야 합니다.");
    }
    return value;
  }
}
