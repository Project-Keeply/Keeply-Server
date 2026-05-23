package com.keeply.auth.kakao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KakaoAuthClient {
  private final KakaoProperties kakaoProperties;
  private final RestClient restClient = RestClient.create();

  public KakaoTokenResponse getAccessToken(String code) {
    return restClient
        .post()
        .uri("https://kauth.kakao.com/oauth/token")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(
            "grant_type=authorization_code"
                + "&client_id="
                + kakaoProperties.getClientId()
                + "&redirect_uri="
                + kakaoProperties.getRedirectUri()
                + "&code="
                + code
                + "&client_secret="
                + kakaoProperties.getClientSecret())
        .retrieve()
        .body(KakaoTokenResponse.class);
  }

  public KakaoUserInfoResponse getUserInfo(String accessToken) {
    return restClient
        .get()
        .uri("https://kapi.kakao.com/v2/user/me")
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .body(KakaoUserInfoResponse.class);
  }
}
