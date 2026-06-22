package com.keeply.group.repository;

import com.keeply.group.entity.GroupMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
  Optional<GroupMember> findByUserId(Long userId);

  Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

  boolean existsByUserId(Long userId);

  boolean existsByGroupIdAndUserId(Long groupId, Long userId);

  List<GroupMember> findByGroupId(Long groupId);

  @Modifying
  @Query("DELETE FROM GroupMember gm WHERE gm.group.id = :groupId")
  void deleteByGroupId(@Param("groupId") Long groupId);
}
