package com.keeply.auth.kakao;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KakaoAuthClient {

  private final KakaoProperties kakaoProperties;
  private final RestClient restClient = RestClient.create();

  public KakaoTokenResponse getAccessToken(String code) {
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "authorization_code");
    body.add("client_id", kakaoProperties.getClientId());
    body.add("redirect_uri", kakaoProperties.getRedirectUri());
    body.add("code", code);
    body.add("client_secret", kakaoProperties.getClientSecret());

    return restClient
        .post()
        .uri("https://kauth.kakao.com/oauth/token")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(body)
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
