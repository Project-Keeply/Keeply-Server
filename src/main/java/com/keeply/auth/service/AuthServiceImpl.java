package com.keeply.auth.service;

import com.keeply.auth.dto.LoginResponse;
import com.keeply.auth.kakao.KakaoAuthClient;
import com.keeply.auth.kakao.KakaoUserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private final KakaoAuthClient kakaoAuthClient;
  private final AuthAccountService authAccountService;

  @Override
  public LoginResponse login(String code) {
    String kakaoAccessToken = kakaoAuthClient.getAccessToken(code).getAccessToken();
    KakaoUserInfoResponse userInfo = kakaoAuthClient.getUserInfo(kakaoAccessToken);
    return authAccountService.processLogin(userInfo);
  }

  @Override
  public LoginResponse reissue(String refreshToken) {
    return authAccountService.rotateRefreshToken(refreshToken);
  }

  @Override
  public void logoutSession(String refreshToken) {
    authAccountService.logoutSession(refreshToken);
  }

  @Override
  public void logout(Long userId) {
    authAccountService.logout(userId);
  }
}
