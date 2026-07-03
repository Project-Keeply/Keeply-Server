package com.keeply.expiry.repository;

import com.keeply.expiry.entity.ExpiryItem;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpiryItemRepository extends JpaRepository<ExpiryItem, Long> {

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Optional<ExpiryItem> findByIdAndGroup_Id(Long expiryItemId, Long groupId);

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Page<ExpiryItem> findByGroup_Id(Long groupId, Pageable pageable);

  @EntityGraph(attributePaths = {"authorMember", "authorMember.user"})
  Page<ExpiryItem> findByGroup_IdAndExpireDateBetween(
      Long groupId, LocalDate from, LocalDate to, Pageable pageable);
}
