package com.keeply.worklog.repository;

import com.keeply.worklog.entity.WorkLog;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Optional<WorkLog> findByIdAndGroup_Id(Long workLogId, Long groupId);

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Page<WorkLog> findByGroup_Id(Long groupId, Pageable pageable);

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Page<WorkLog> findByGroup_IdAndCreatedAtGreaterThanEqual(
      Long groupId, LocalDateTime from, Pageable pageable);

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Page<WorkLog> findByGroup_IdAndCreatedAtLessThan(
      Long groupId, LocalDateTime toExclusive, Pageable pageable);

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Page<WorkLog> findByGroup_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Long groupId, LocalDateTime from, LocalDateTime toExclusive, Pageable pageable);
}
