package com.keeply.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.keeply.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  @Autowired private TestEntityManager entityManager;

  @Test
  void findForUpdateReturnsActiveUserByIdAndKakaoId() {
    User user = entityManager.persistAndFlush(User.builder().kakaoId("active").name("사용자").build());
    Long userId = user.getId();
    entityManager.clear();

    assertThat(userRepository.findByIdForUpdate(userId))
        .hasValueSatisfying(found -> assertThat(found.getKakaoId()).isEqualTo("active"));
    assertThat(userRepository.findByKakaoIdForUpdate("active"))
        .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(userId));
  }

  @Test
  void findForUpdateIncludesDeletedUserForStateCheckAndRestore() {
    User user = User.builder().kakaoId("deleted").name("탈퇴 사용자").build();
    user.markDeleted();
    entityManager.persistAndFlush(user);
    Long userId = user.getId();
    entityManager.clear();

    assertThat(userRepository.findByIdForUpdate(userId))
        .hasValueSatisfying(found -> assertThat(found.isDeleted()).isTrue());
    assertThat(userRepository.findByKakaoIdForUpdate("deleted"))
        .hasValueSatisfying(found -> assertThat(found.isDeleted()).isTrue());
    assertThat(userRepository.findByIdAndDeletedAtIsNull(userId)).isEmpty();
  }

  @Test
  void findForUpdateReturnsEmptyForMissingUser() {
    assertThat(userRepository.findByIdForUpdate(Long.MAX_VALUE)).isEmpty();
    assertThat(userRepository.findByKakaoIdForUpdate("missing")).isEmpty();
  }
}
