package com.keeply.group.repository;

import com.keeply.group.entity.GroupMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
  Optional<GroupMember> findByUserId(Long userId);

  boolean existsByUserId(Long userId);

  boolean existsByGroupIdAndUserId(Long groupId, Long userId);

  List<GroupMember> findByGroupId(Long groupId);

  void deleteByGroupId(Long groupId);
}
