package com.keeply.worklog.controller;

import com.keeply.common.response.ApiResponse;
import com.keeply.common.response.PageResponse;
import com.keeply.common.security.annotation.GroupMemberOnly;
import com.keeply.worklog.dto.request.CreateWorkLogRequest;
import com.keeply.worklog.dto.request.UpdateWorkLogRequest;
import com.keeply.worklog.dto.response.WorkLogResponse;
import com.keeply.worklog.service.WorkLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/groups/{groupId}/work-logs")
@Tag(name = "WorkLog", description = "운영 로그 API")
public class WorkLogController {

  private final WorkLogService workLogService;

  @GroupMemberOnly
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "운영 로그 작성")
  public ApiResponse<WorkLogResponse> createWorkLog(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long groupId,
      @RequestBody @Valid CreateWorkLogRequest request) {
    WorkLogResponse response = workLogService.createWorkLog(userId, groupId, request);
    return ApiResponse.success(response);
  }

  @GroupMemberOnly
  @GetMapping
  @Operation(summary = "운영 로그 목록 조회")
  public ApiResponse<PageResponse<WorkLogResponse>> getWorkLogList(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long groupId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @PageableDefault
          @SortDefault.SortDefaults({
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC),
            @SortDefault(sort = "id", direction = Sort.Direction.DESC)
          })
          Pageable pageable) {
    PageResponse<WorkLogResponse> response =
        workLogService.getWorkLogList(groupId, from, to, pageable);
    return ApiResponse.success(response);
  }

  @GroupMemberOnly
  @GetMapping("/{workLogId}")
  @Operation(summary = "운영 로그 상세 조회")
  public ApiResponse<WorkLogResponse> getWorkLog(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long groupId,
      @PathVariable Long workLogId) {
    WorkLogResponse response = workLogService.getWorkLog(groupId, workLogId);
    return ApiResponse.success(response);
  }

  @GroupMemberOnly
  @PatchMapping("/{workLogId}")
  @Operation(summary = "운영 로그 수정")
  public ApiResponse<WorkLogResponse> updateWorkLog(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long groupId,
      @PathVariable Long workLogId,
      @RequestBody @Valid UpdateWorkLogRequest request) {
    WorkLogResponse response = workLogService.updateWorkLog(userId, groupId, workLogId, request);
    return ApiResponse.success(response);
  }

  @GroupMemberOnly
  @DeleteMapping("/{workLogId}")
  @Operation(summary = "운영 로그 삭제")
  public ApiResponse<Void> deleteWorkLog(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long groupId,
      @PathVariable Long workLogId) {
    workLogService.deleteWorkLog(userId, groupId, workLogId);
    return ApiResponse.success(null);
  }
}
