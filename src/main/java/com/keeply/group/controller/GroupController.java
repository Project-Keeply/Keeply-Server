package com.keeply.group.controller;

import com.keeply.common.response.ApiResponse;
import com.keeply.group.dto.request.UpdateGroupRequest;
import com.keeply.group.dto.response.GroupResponse;
import com.keeply.group.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/groups")
public class GroupController {

  private final GroupService groupService;

  @GetMapping("/me")
  public ApiResponse<GroupResponse> getMyGroup(@AuthenticationPrincipal Long userId) {
    GroupResponse response = groupService.getMyGroup(userId);
    return ApiResponse.success(response);
  }

  @PatchMapping("/me")
  public ApiResponse<GroupResponse> updateMyGroup(
      @AuthenticationPrincipal Long userId, @RequestBody @Valid UpdateGroupRequest request) {
    GroupResponse response = groupService.updateMyGroup(userId, request);
    return ApiResponse.success(response);
  }

  @DeleteMapping("/me")
  public ApiResponse<Void> deleteMyGroup(@AuthenticationPrincipal Long userId) {
    groupService.deleteMyGroup(userId);
    return ApiResponse.success(null);
  }

  @PatchMapping("/me/invite-code")
  public ApiResponse<GroupResponse> reissueInviteCode(@AuthenticationPrincipal Long userId) {
    GroupResponse response = groupService.reissueInviteCode(userId);
    return ApiResponse.success(response);
  }

  @PostMapping("/me/leave")
  public ApiResponse<Void> leaveMyGroup(@AuthenticationPrincipal Long userId) {
    groupService.leaveMyGroup(userId);
    return ApiResponse.success(null);
  }
}
