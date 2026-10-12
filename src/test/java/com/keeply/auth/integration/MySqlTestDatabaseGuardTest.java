package com.keeply.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MySqlTestDatabaseGuardTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "jdbc:mysql://localhost:3306/keeply_auth_test_rotation",
        "jdbc:mysql://127.0.0.1:13306/keeply_auth_test_case_1"
      })
  void permitsOnlyExplicitLoopbackDedicatedDatabase(String url) {
    assertThat(MySqlTestDatabaseGuard.requireDedicatedDatabaseUrl(url)).isEqualTo(url);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "jdbc:mysql://localhost:3306/keeply_dev",
        "jdbc:mysql://localhost:3306/keeply_auth_test_",
        "jdbc:mysql://production:3306/keeply_auth_test_rotation",
        "jdbc:mysql://127.0.0.1:3306/keeply_auth_test_rotation?allowMultiQueries=true",
        "jdbc:mysql://localhost:99999/keeply_auth_test_rotation",
        "jdbc:mysql://localhost:3306/keeply_auth_test_rotation/../keeply_dev",
        "jdbc:mysql://localhost:3306/keeply_auth_test_rotation\n"
      })
  void rejectsUnsafeDatabaseBeforeSpringContextStarts(String url) {
    assertThatThrownBy(() -> MySqlTestDatabaseGuard.requireDedicatedDatabaseUrl(url))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"8.0.46", "8.4.6-commercial"})
  void permitsMySql8ServerVersions(String version) {
    assertThat(MySqlTestDatabaseGuard.requireMySql8Version(version)).isEqualTo(version);
  }

  @ParameterizedTest
  @ValueSource(strings = {"5.7.44", "8.0.46-MariaDB"})
  void rejectsWrongMajorVersionOrMariaDb(String version) {
    assertThatThrownBy(() -> MySqlTestDatabaseGuard.requireMySql8Version(version))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsMissingCredentialsWithoutPrintingValues() {
    assertThatThrownBy(() -> MySqlTestDatabaseGuard.requireCredential(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MySqlTestDatabaseGuard.requireCredential("  "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
