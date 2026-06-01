package com.keeply.group.controller;

import com.keeply.common.response.ApiResponse;
import com.keeply.group.dto.response.GroupResponse;
import com.keeply.group.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}
