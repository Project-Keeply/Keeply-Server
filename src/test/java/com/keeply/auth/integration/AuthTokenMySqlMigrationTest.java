package com.keeply.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** 사전에 생성한 서로 다른 빈 로컬 DB 두 개에서 실제 Flyway를 검증한다. DB 생성/삭제/clean은 실행하지 않는다. */
class AuthTokenMySqlMigrationTest {
  private static final String NEW_URL = "KEEPLY_AUTH_MYSQL_MIGRATION_NEW_URL";
  private static final String EXISTING_URL = "KEEPLY_AUTH_MYSQL_MIGRATION_EXISTING_URL";
  private static final List<String> PRESERVED_TABLES =
      List.of("users", "store_groups", "group_members", "notices", "work_logs", "expiry_items");

  @Test
  @EnabledIfEnvironmentVariable(named = NEW_URL, matches = ".+")
  void migratesEmptyDatabaseThroughAllSevenVersions() {
    DedicatedDatabase database = getDedicatedDatabase(NEW_URL, EXISTING_URL);
    requireEmptyDatabase(database);
    Flyway flyway = createFlyway(database, "latest");

    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(7);
    flyway.validate();
    assertMigrationHistory(database, List.of("1", "2", "3", "4", "5", "6", "7"));
    assertRefreshSessionSchema(database);
    assertThat(flyway.migrate().migrationsExecuted).isZero();
  }

  @Test
  @EnabledIfEnvironmentVariable(named = EXISTING_URL, matches = ".+")
  void migratesExistingVersionSixDataAndInvalidatesOnlyRefreshSessions() {
    DedicatedDatabase database = getDedicatedDatabase(EXISTING_URL, NEW_URL);
    requireEmptyDatabase(database);
    Flyway previousFlyway = createFlyway(database, "6");
    assertThat(previousFlyway.migrate().migrationsExecuted).isEqualTo(6);
    previousFlyway.validate();
    assertMigrationHistory(database, List.of("1", "2", "3", "4", "5", "6"));
    createVersionSixFixtures(database);
    Map<String, List<List<String>>> before = getPreservedData(database);
    assertThat(queryRows(database, "SELECT COUNT(*) FROM refresh_token"))
        .containsExactly(List.of("1"));

    Flyway latestFlyway = createFlyway(database, "latest");
    assertThat(latestFlyway.migrate().migrationsExecuted).isEqualTo(1);
    latestFlyway.validate();

    assertThat(getPreservedData(database)).isEqualTo(before);
    assertThat(queryRows(database, "SELECT COUNT(*) FROM refresh_token"))
        .containsExactly(List.of("0"));
    assertMigrationHistory(database, List.of("1", "2", "3", "4", "5", "6", "7"));
    assertRefreshSessionSchema(database);
    createNewRefreshSession(database);
    assertThat(latestFlyway.migrate().migrationsExecuted).isZero();
    assertThat(queryRows(database, "SELECT COUNT(*) FROM refresh_token"))
        .containsExactly(List.of("1"));
  }

  private DedicatedDatabase getDedicatedDatabase(String selectedVariable, String otherVariable) {
    String url =
        MySqlTestDatabaseGuard.requireDedicatedDatabaseUrl(System.getenv(selectedVariable));
    String otherUrl = System.getenv(otherVariable);
    if (otherUrl != null && !otherUrl.isBlank()) {
      MySqlTestDatabaseGuard.requireDedicatedDatabaseUrl(otherUrl);
      // localhost와 127.0.0.1을 바꾼 동일 DB도 거부한다.
      String databaseAddress = url.replace("//localhost:", "//127.0.0.1:");
      String otherDatabaseAddress = otherUrl.replace("//localhost:", "//127.0.0.1:");
      if (databaseAddress.equals(otherDatabaseAddress)) {
        throw new IllegalArgumentException("신규/기존 마이그레이션 검증은 서로 다른 전용 DB가 필요합니다.");
      }
    }
    String username =
        MySqlTestDatabaseGuard.requireCredential(System.getenv("KEEPLY_AUTH_MYSQL_TEST_USERNAME"));
    String password =
        MySqlTestDatabaseGuard.requireCredential(System.getenv("KEEPLY_AUTH_MYSQL_TEST_PASSWORD"));
    DedicatedDatabase database = new DedicatedDatabase(url, username, password);
    try (Connection connection = createConnection(database)) {
      connection.setReadOnly(true);
      if (!"MySQL".equals(connection.getMetaData().getDatabaseProductName())) {
        throw new IllegalArgumentException("마이그레이션 테스트는 실제 MySQL 서버만 허용합니다.");
      }
      MySqlTestDatabaseGuard.requireMySql8Version(
          connection.getMetaData().getDatabaseProductVersion());
      return database;
    } catch (SQLException exception) {
      throw sanitizeSqlException(exception);
    }
  }

  private void requireEmptyDatabase(DedicatedDatabase database) {
    assertThat(
            queryRows(
                database,
                "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()"))
        .as("테스트는 테이블이 전혀 없는 사전 생성 전용 DB에서만 실행한다")
        .isEmpty();
  }

  private Flyway createFlyway(DedicatedDatabase database, String target) {
    return Flyway.configure()
        .dataSource(database.url(), database.username(), database.password())
        .jdbcProperties(Map.of("connectTimeout", "3000", "socketTimeout", "3000"))
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .baselineOnMigrate(false)
        .target(target)
        .load();
  }

  private void assertMigrationHistory(DedicatedDatabase database, List<String> versions) {
    assertThat(
            queryRows(
                    database,
                    "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank")
                .stream()
                .map(List::getFirst)
                .toList())
        .containsExactlyElementsOf(versions);
    assertThat(queryRows(database, "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0"))
        .containsExactly(List.of("0"));
  }

  private void assertRefreshSessionSchema(DedicatedDatabase database) {
    assertThat(
            queryRows(
                database,
                "SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE"
                    + " FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()"
                    + " AND TABLE_NAME = 'refresh_token' AND COLUMN_NAME = 'session_id'"))
        .containsExactly(List.of("varchar", "36", "NO"));
    assertThat(
            queryRows(
                database,
                "SELECT INDEX_NAME, NON_UNIQUE, COLUMN_NAME"
                    + " FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()"
                    + " AND TABLE_NAME = 'refresh_token' AND INDEX_NAME = 'uq_refresh_token_user_id'"))
        .containsExactly(List.of("uq_refresh_token_user_id", "0", "user_id"));
    assertThat(
            queryRows(
                database,
                "SELECT COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME"
                    + " FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE()"
                    + " AND TABLE_NAME = 'refresh_token' AND CONSTRAINT_NAME = 'fk_refresh_token_user'"))
        .containsExactly(List.of("user_id", "users", "id"));
  }

  private void createVersionSixFixtures(DedicatedDatabase database) {
    executeSql(
        database,
        "INSERT INTO users (id, kakao_id, name, profile_image_url, is_name_customized, created_at)"
            + " VALUES (1, 'migration-fixture', '검증 사용자', 'https://example.test/profile', b'0', '2026-10-11 01:00:00')",
        "INSERT INTO refresh_token (user_id, token, expiry_date)"
            + " VALUES (1, 'old-refresh-session', '2026-10-25 01:00:00')",
        "INSERT INTO store_groups (id, name, store_brand, invite_code, created_at)"
            + " VALUES (1, '검증 그룹', 'CU', 'migration-fixture', '2026-10-11 01:00:00')",
        "INSERT INTO group_members (group_id, user_id, role, joined_at)"
            + " VALUES (1, 1, 'OWNER', '2026-10-11 01:00:00')",
        "INSERT INTO notices (group_id, author_user_id, title, content, tag, image_url, created_at)"
            + " VALUES (1, 1, '검증 공지', '공지 보존', 'DAILY', 'https://example.test/notice', '2026-10-11 01:00:00')",
        "INSERT INTO work_logs (group_id, author_user_id, content, created_at)"
            + " VALUES (1, 1, '업무 기록 보존', '2026-10-11 01:00:00')",
        "INSERT INTO expiry_items (group_id, author_user_id, product_name, expire_date, category, image_url, created_at)"
            + " VALUES (1, 1, '검증 상품', '2026-10-20', 'ETC', 'https://example.test/product', '2026-10-11 01:00:00')");
  }

  private Map<String, List<List<String>>> getPreservedData(DedicatedDatabase database) {
    Map<String, List<List<String>>> snapshot = new LinkedHashMap<>();
    PRESERVED_TABLES.forEach(
        table -> {
          List<List<String>> rows = queryRows(database, "SELECT * FROM " + table + " ORDER BY id");
          assertThat(rows).as("V6 fixture %s", table).hasSize(1);
          snapshot.put(table, rows);
        });
    return snapshot;
  }

  private void createNewRefreshSession(DedicatedDatabase database) {
    String insert =
        "INSERT INTO refresh_token (user_id, token, expiry_date, session_id) VALUES"
            + " (1, '"
            + "a".repeat(64)
            + "', '2026-10-25 01:00:00',";
    assertThatThrownBy(() -> executeSql(database, insert + " NULL)"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SQLState=23000", "errorCode=1048");
    executeSql(database, insert + " '64000000-0000-4000-8000-000000000001')");
    assertThat(queryRows(database, "SELECT LENGTH(token), LENGTH(session_id) FROM refresh_token"))
        .containsExactly(List.of("64", "36"));
  }

  private Connection createConnection(DedicatedDatabase database) throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("user", database.username());
    properties.setProperty("password", database.password());
    properties.setProperty("connectTimeout", "3000");
    properties.setProperty("socketTimeout", "3000");
    return DriverManager.getConnection(database.url(), properties);
  }

  private List<List<String>> queryRows(DedicatedDatabase database, String sql) {
    try (Connection connection = createConnection(database);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      ResultSetMetaData metadata = resultSet.getMetaData();
      List<List<String>> rows = new ArrayList<>();
      while (resultSet.next()) {
        List<String> row = new ArrayList<>();
        for (int column = 1; column <= metadata.getColumnCount(); column++) {
          row.add(resultSet.getString(column));
        }
        rows.add(row);
      }
      return rows;
    } catch (SQLException exception) {
      throw sanitizeSqlException(exception);
    }
  }

  private void executeSql(DedicatedDatabase database, String... statements) {
    try (Connection connection = createConnection(database);
        Statement statement = connection.createStatement()) {
      for (String sql : statements) {
        statement.executeUpdate(sql);
      }
    } catch (SQLException exception) {
      throw sanitizeSqlException(exception);
    }
  }

  private IllegalStateException sanitizeSqlException(SQLException exception) {
    // JDBC 오류 원문/원인은 접속 자격 증명을 포함할 수 있으므로 출력하지 않는다.
    return new IllegalStateException(
        "MySQL 검증 SQL 실행 실패: SQLState="
            + exception.getSQLState()
            + ", errorCode="
            + exception.getErrorCode());
  }

  private record DedicatedDatabase(String url, String username, String password) {}
}
