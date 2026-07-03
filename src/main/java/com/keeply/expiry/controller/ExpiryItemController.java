package com.keeply.expiry.controller;

import com.keeply.common.response.ApiResponse;
import com.keeply.common.response.PageResponse;
import com.keeply.common.security.annotation.GroupMemberOnly;
import com.keeply.expiry.dto.request.CreateExpiryItemRequest;
import com.keeply.expiry.dto.request.UpdateExpiryItemRequest;
import com.keeply.expiry.dto.response.ExpiryItemResponse;
import com.keeply.expiry.service.ExpiryItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/groups/{groupId}/expiry-items")
@Tag(name = "ExpiryItem", description = "유통기한 상품 API")
public class ExpiryItemController {

  private final ExpiryItemService expiryItemService;

  @GroupMemberOnly
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "유통기한 상품 등록")
  public ApiResponse<ExpiryItemResponse> createExpiryItem(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long groupId,
      @RequestBody @Valid CreateExpiryItemRequest request) {
    ExpiryItemResponse response = expiryItemService.createExpiryItem(userId, groupId, request);
    return ApiResponse.success(response);
  }

  @GroupMemberOnly
  @GetMapping
  @Operation(summary = "유통기한 상품 목록 조회")
  public ApiResponse<PageResponse<ExpiryItemResponse>> getExpiryItemList(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long groupId,
      @RequestParam(required = false) Integer withinDays,
      @PageableDefault
          @SortDefault.SortDefaults({
            @SortDefault(sort = "expireDate", direction = Sort.Direction.ASC),
            @SortDefault(sort = "id", direction = Sort.Direction.ASC)
          })
          Pageable pageable) {
    PageResponse<ExpiryItemResponse> response =
        expiryItemService.getExpiryItemList(groupId, withinDays, pageable);
    return ApiResponse.success(response);
  }

  @GroupMemberOnly
  @GetMapping("/{itemId}")
  @Operation(summary = "유통기한 상품 상세 조회")
  public ApiResponse<ExpiryItemResponse> getExpiryItem(
      @AuthenticationPrincipal Long userId, @PathVariable Long groupId, @PathVariable Long itemId) {
    ExpiryItemResponse response = expiryItemService.getExpiryItem(groupId, itemId);
    return ApiResponse.success(response);
  }

  @GroupMemberOnly
  @PatchMapping("/{itemId}")
  @Operation(summary = "유통기한 상품 수정")
  public ApiResponse<ExpiryItemResponse> updateExpiryItem(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long groupId,
      @PathVariable Long itemId,
      @RequestBody @Valid UpdateExpiryItemRequest request) {
    ExpiryItemResponse response =
        expiryItemService.updateExpiryItem(userId, groupId, itemId, request);
    return ApiResponse.success(response);
  }

  @GroupMemberOnly
  @DeleteMapping("/{itemId}")
  @Operation(summary = "유통기한 상품 삭제")
  public ApiResponse<Void> deleteExpiryItem(
      @AuthenticationPrincipal Long userId, @PathVariable Long groupId, @PathVariable Long itemId) {
    expiryItemService.deleteExpiryItem(userId, groupId, itemId);
    return ApiResponse.success(null);
  }
}
