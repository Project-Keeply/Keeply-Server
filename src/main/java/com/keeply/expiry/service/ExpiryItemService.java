package com.keeply.expiry.service;

import com.keeply.common.response.PageResponse;
import com.keeply.expiry.dto.request.CreateExpiryItemRequest;
import com.keeply.expiry.dto.request.UpdateExpiryItemRequest;
import com.keeply.expiry.dto.response.ExpiryItemResponse;
import org.springframework.data.domain.Pageable;

public interface ExpiryItemService {

  ExpiryItemResponse createExpiryItem(Long userId, Long groupId, CreateExpiryItemRequest request);

  PageResponse<ExpiryItemResponse> getExpiryItemList(
      Long groupId, Integer withinDays, Pageable pageable);

  ExpiryItemResponse getExpiryItem(Long groupId, Long itemId);

  ExpiryItemResponse updateExpiryItem(
      Long userId, Long groupId, Long itemId, UpdateExpiryItemRequest request);

  void deleteExpiryItem(Long userId, Long groupId, Long itemId);
}
