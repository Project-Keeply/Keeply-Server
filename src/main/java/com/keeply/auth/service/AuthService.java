package com.keeply.auth.service;

import com.keeply.auth.dto.LoginResponse;
import com.keeply.auth.kakao.KakaoAuthClient;
import com.keeply.auth.kakao.KakaoUserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final KakaoAuthClient kakaoAuthClient;
  private final AuthAccountService authAccountService;

  public LoginResponse login(String code) {
    String kakaoAccessToken = kakaoAuthClient.getAccessToken(code).getAccessToken();
    KakaoUserInfoResponse userInfo = kakaoAuthClient.getUserInfo(kakaoAccessToken);
    return authAccountService.processLogin(userInfo);
  }

  public LoginResponse reissue(String refreshToken) {
    return authAccountService.rotateRefreshToken(refreshToken);
  }

  public void logout(Long userId) {
    authAccountService.logout(userId);
  }
}
