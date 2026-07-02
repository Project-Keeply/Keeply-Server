package com.keeply.group.controller;

import com.keeply.common.response.ApiResponse;
import com.keeply.common.security.annotation.GroupMemberOnly;
import com.keeply.group.dto.response.GroupMemberResponse;
import com.keeply.group.service.GroupMemberService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/groups/{groupId}/members")
public class GroupMemberController {
  private final GroupMemberService groupMemberService;

  @GroupMemberOnly
  @GetMapping
  public ApiResponse<List<GroupMemberResponse>> getList(
      @AuthenticationPrincipal Long userId, @PathVariable Long groupId) {
    return ApiResponse.success(groupMemberService.getList(userId, groupId));
  }

  @GroupMemberOnly
  @DeleteMapping("/me")
  public ApiResponse<Void> leave(@AuthenticationPrincipal Long userId, @PathVariable Long groupId) {
    groupMemberService.leave(userId, groupId);
    return ApiResponse.success(null);
  }
}
