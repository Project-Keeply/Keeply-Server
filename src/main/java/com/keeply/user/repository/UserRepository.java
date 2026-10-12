package com.keeply.user.repository;

import com.keeply.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByKakaoId(String kakaoId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select user from User user where user.id = :id")
  Optional<User> findByIdForUpdate(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select user from User user where user.kakaoId = :kakaoId")
  Optional<User> findByKakaoIdForUpdate(@Param("kakaoId") String kakaoId);

  Optional<User> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByIdAndDeletedAtIsNull(Long id);
}
