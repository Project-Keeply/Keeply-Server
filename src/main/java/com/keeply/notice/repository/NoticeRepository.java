package com.keeply.notice.repository;

import com.keeply.notice.entity.Notice;
import com.keeply.notice.entity.NoticeTag;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Optional<Notice> findByIdAndGroup_Id(Long noticeId, Long groupId);

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Page<Notice> findByGroup_Id(Long groupId, Pageable pageable);

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Page<Notice> findByGroup_IdAndTag(Long groupId, NoticeTag tag, Pageable pageable);
}
