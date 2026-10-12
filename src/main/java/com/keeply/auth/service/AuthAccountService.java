package com.keeply.auth.service;

import com.keeply.auth.dto.TokenIssueResult;
import com.keeply.auth.kakao.KakaoUserInfoResponse;

public interface AuthAccountService {
  TokenIssueResult processLogin(KakaoUserInfoResponse userInfo);

  TokenIssueResult rotateRefreshToken(String refreshToken);

  void logoutSession(String refreshToken);
}
