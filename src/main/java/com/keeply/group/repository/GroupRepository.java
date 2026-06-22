package com.keeply.group.repository;

import com.keeply.group.entity.Group;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
  Optional<Group> findByInviteCode(String inviteCode);

  boolean existsByInviteCode(String inviteCode);
}
