package com.keeply.user.controller;

import com.keeply.common.response.ApiResponse;
import com.keeply.user.dto.request.UpdateUserRequest;
import com.keeply.user.dto.response.UserResponse;
import com.keeply.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal Long userId) {
    UserResponse response = userService.getMe(userId);
    return ApiResponse.success(response);
  }

  @PatchMapping("/me")
  public ApiResponse<UserResponse> updateName(
      @AuthenticationPrincipal Long userId, @RequestBody @Valid UpdateUserRequest request) {
    UserResponse response = userService.updateName(userId, request);
    return ApiResponse.success(response);
  }
}
