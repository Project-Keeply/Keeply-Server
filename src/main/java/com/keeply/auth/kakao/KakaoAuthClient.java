package com.keeply.auth.kakao;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

    try {
      return restClient
          .post()
          .uri("https://kauth.kakao.com/oauth/token")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(body)
          .retrieve()
          .body(KakaoTokenResponse.class);
    } catch (RestClientException e) {
      throw new CustomException(ErrorCode.KAKAO_AUTH_FAILED);
    }
  }

  public KakaoUserInfoResponse getUserInfo(String accessToken) {
    try {
      return restClient
          .get()
          .uri("https://kapi.kakao.com/v2/user/me")
          .header("Authorization", "Bearer " + accessToken)
          .retrieve()
          .body(KakaoUserInfoResponse.class);
    } catch (RestClientException e) {
      throw new CustomException(ErrorCode.KAKAO_AUTH_FAILED);
    }
  }
}
