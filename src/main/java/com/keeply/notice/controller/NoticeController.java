package com.keeply.notice.controller;

import com.keeply.common.response.ApiResponse;
import com.keeply.common.response.PageResponse;
import com.keeply.common.security.annotation.GroupMemberOnly;
import com.keeply.notice.dto.request.CreateNoticeRequest;
import com.keeply.notice.dto.request.UpdateNoticeRequest;
import com.keeply.notice.dto.response.NoticeListResponse;
import com.keeply.notice.dto.response.NoticeResponse;
import com.keeply.notice.entity.NoticeTag;
import com.keeply.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/groups/{groupId}/notices")
@Tag(name = "Notice", description = "공지 API")
public class NoticeController {

  private final NoticeService noticeService;

  @GroupMemberOnly
  @PostMapping
  @Operation(summary = "공지 등록")
  public ApiResponse<NoticeResponse> createNotice(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long groupId,
      @RequestBody @Valid CreateNoticeRequest request) {
    NoticeResponse response = noticeService.createNotice(userId, groupId, request);
    return ApiResponse.success(response);
  }

  @GroupMemberOnly
  @GetMapping
  @Operation(summary = "공지 목록 조회")
  public ApiResponse<PageResponse<NoticeListResponse>> getNoticeList(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long groupId,
      @RequestParam(required = false) NoticeTag tag,
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    PageResponse<NoticeListResponse> response = noticeService.getNoticeList(groupId, tag, pageable);
    return ApiResponse.success(response);
  }

  @GroupMemberOnly
  @GetMapping("/{noticeId}")
  @Operation(summary = "공지 상세 조회")
  public ApiResponse<NoticeResponse> getNotice(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long groupId,
      @PathVariable Long noticeId) {
    NoticeResponse response = noticeService.getNotice(groupId, noticeId);
    return ApiResponse.success(response);
  }

  @GroupMemberOnly
  @PatchMapping("/{noticeId}")
  @Operation(summary = "공지 수정")
  public ApiResponse<NoticeResponse> updateNotice(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long groupId,
      @PathVariable Long noticeId,
      @RequestBody @Valid UpdateNoticeRequest request) {
    NoticeResponse response = noticeService.updateNotice(userId, groupId, noticeId, request);
    return ApiResponse.success(response);
  }

  @GroupMemberOnly
  @DeleteMapping("/{noticeId}")
  @Operation(summary = "공지 삭제")
  public ApiResponse<Void> deleteNotice(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long groupId,
      @PathVariable Long noticeId) {
    noticeService.deleteNotice(userId, groupId, noticeId);
    return ApiResponse.success(null);
  }
}
