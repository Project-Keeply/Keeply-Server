package com.keeply.notice.repository;

import com.keeply.notice.entity.Notice;
import com.keeply.notice.entity.NoticeTag;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Optional<Notice> findByIdAndGroup_Id(Long noticeId, Long groupId);

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Page<Notice> findByGroup_Id(Long groupId, Pageable pageable);

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Page<Notice> findByGroup_IdAndTag(Long groupId, NoticeTag tag, Pageable pageable);

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  @Query(
      """
      SELECT notice
      FROM Notice notice
      WHERE notice.group.id = :groupId
        AND (
          (notice.tag = :dailyTag
            AND notice.createdAt >= :dailyStartAt
            AND notice.createdAt < :dailyEndAt)
          OR (notice.tag = :weeklyTag
            AND notice.createdAt >= :weeklyStartAt
            AND notice.createdAt < :weeklyEndAt)
        )
      """)
  Page<Notice> findActiveByGroup_Id(
      @Param("groupId") Long groupId,
      @Param("dailyTag") NoticeTag dailyTag,
      @Param("dailyStartAt") LocalDateTime dailyStartAt,
      @Param("dailyEndAt") LocalDateTime dailyEndAt,
      @Param("weeklyTag") NoticeTag weeklyTag,
      @Param("weeklyStartAt") LocalDateTime weeklyStartAt,
      @Param("weeklyEndAt") LocalDateTime weeklyEndAt,
      Pageable pageable);

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Page<Notice> findByGroup_IdAndTagAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Long groupId, NoticeTag tag, LocalDateTime startAt, LocalDateTime endAt, Pageable pageable);
}
