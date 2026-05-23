package com.keeply.auth.controller;

import com.keeply.auth.dto.KakaoLoginRequest;
import com.keeply.auth.dto.LoginResponse;
import com.keeply.auth.dto.ReissueRequest;
import com.keeply.auth.service.AuthService;
import com.keeply.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/kakao/callback")
  public ApiResponse<LoginResponse> kakaoLogin(@RequestBody @Valid KakaoLoginRequest request) {
    LoginResponse response = authService.login(request.getCode());
    return ApiResponse.success(response);
  }

  @PostMapping("/refresh")
  public ApiResponse<LoginResponse> reissue(@RequestBody @Valid ReissueRequest request) {
    LoginResponse response = authService.reissue(request.getRefreshToken());
    return ApiResponse.success(response);
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(@AuthenticationPrincipal Long userId) {
    authService.logout(userId);
    return ApiResponse.success(null);
  }
}
