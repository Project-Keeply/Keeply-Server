package com.keeply.auth.service;

import com.keeply.auth.dto.LoginResponse;

public interface AuthService {
  LoginResponse login(String code);

  LoginResponse reissue(String refreshToken);

  void logoutSession(String refreshToken);

  // 기존 Controller가 쿠키 기반 로그아웃으로 전환될 때 제거한다.
  void logout(Long userId);
}
