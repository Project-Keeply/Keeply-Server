package com.keeply.user.repository;

import com.keeply.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByKakaoId(String kakaoId);
}
