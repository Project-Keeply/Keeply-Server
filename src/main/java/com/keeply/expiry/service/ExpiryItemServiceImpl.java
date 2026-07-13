package com.keeply.expiry.service;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.common.response.PageResponse;
import com.keeply.expiry.dto.request.CreateExpiryItemRequest;
import com.keeply.expiry.dto.request.UpdateExpiryItemRequest;
import com.keeply.expiry.dto.response.ExpiryItemResponse;
import com.keeply.expiry.entity.ExpiryItem;
import com.keeply.expiry.repository.ExpiryItemRepository;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.repository.GroupMemberRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpiryItemServiceImpl implements ExpiryItemService {

  private final ExpiryItemRepository expiryItemRepository;
  private final GroupMemberRepository groupMemberRepository;

  @Override
  @Transactional
  public ExpiryItemResponse createExpiryItem(
      Long userId, Long groupId, CreateExpiryItemRequest request) {
    GroupMember authorMember = getGroupMember(groupId, userId);
    ExpiryItem expiryItem =
        ExpiryItem.builder()
            .authorMember(authorMember)
            .productName(request.getProductName())
            .expireDate(request.getExpireDate())
            .category(request.getCategory())
            .imageUrl(request.getImageUrl())
            .build();
    ExpiryItem savedExpiryItem = expiryItemRepository.save(expiryItem);
    return ExpiryItemResponse.of(savedExpiryItem);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ExpiryItemResponse> getExpiryItemList(
      Long groupId, Integer withinDays, Pageable pageable) {
    checkWithinDays(withinDays);
    Page<ExpiryItem> expiryItems =
        withinDays == null
            ? expiryItemRepository.findByGroup_Id(groupId, pageable)
            : getExpiryItemPageWithinDays(groupId, withinDays, pageable);
    return PageResponse.of(expiryItems.map(ExpiryItemResponse::of));
  }

  @Override
  @Transactional(readOnly = true)
  public ExpiryItemResponse getExpiryItem(Long groupId, Long itemId) {
    ExpiryItem expiryItem = getExpiryItemByIdAndGroupId(itemId, groupId);
    return ExpiryItemResponse.of(expiryItem);
  }

  @Override
  @Transactional
  public ExpiryItemResponse updateExpiryItem(
      Long userId, Long groupId, Long itemId, UpdateExpiryItemRequest request) {
    ExpiryItem expiryItem = getExpiryItemByIdAndGroupId(itemId, groupId);
    if (!expiryItem.isAuthor(userId)) {
      throw new CustomException(ErrorCode.NOT_EXPIRY_ITEM_AUTHOR);
    }
    expiryItem.updateInfo(
        request.getProductName(),
        request.getExpireDate(),
        request.getCategory(),
        request.getImageUrl());
    return ExpiryItemResponse.of(expiryItem);
  }

  @Override
  @Transactional
  public void deleteExpiryItem(Long userId, Long groupId, Long itemId) {
    ExpiryItem expiryItem = getExpiryItemByIdAndGroupId(itemId, groupId);
    GroupMember groupMember = getGroupMember(groupId, userId);
    if (!expiryItem.isAuthor(userId) && groupMember.getRole() != GroupRole.OWNER) {
      throw new CustomException(ErrorCode.FORBIDDEN);
    }
    expiryItemRepository.delete(expiryItem);
  }

  private void checkWithinDays(Integer withinDays) {
    if (withinDays != null && withinDays < 0) {
      throw new CustomException(ErrorCode.INVALID_INPUT);
    }
  }

  private Page<ExpiryItem> getExpiryItemPageWithinDays(
      Long groupId, Integer withinDays, Pageable pageable) {
    LocalDate today = LocalDate.now();
    return expiryItemRepository.findByGroup_IdAndExpireDateBetween(
        groupId, today, today.plusDays(withinDays), pageable);
  }

  private ExpiryItem getExpiryItemByIdAndGroupId(Long expiryItemId, Long groupId) {
    return expiryItemRepository
        .findByIdAndGroup_Id(expiryItemId, groupId)
        .orElseThrow(() -> new CustomException(ErrorCode.EXPIRY_ITEM_NOT_FOUND));
  }

  private GroupMember getGroupMember(Long groupId, Long userId) {
    return groupMemberRepository
        .findByGroupIdAndUserId(groupId, userId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
  }
}
