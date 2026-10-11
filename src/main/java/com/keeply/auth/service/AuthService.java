package com.keeply.auth.service;

import com.keeply.auth.dto.TokenIssueResult;

public interface AuthService {
  TokenIssueResult login(String code);

  TokenIssueResult reissue(String refreshToken);

  void logoutSession(String refreshToken);
}
