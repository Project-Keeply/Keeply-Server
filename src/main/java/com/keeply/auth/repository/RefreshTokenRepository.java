package com.keeply.auth.repository;

import com.keeply.auth.entity.RefreshToken;
import com.keeply.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByUser(User user);

  void deleteByUser(User user);
}
