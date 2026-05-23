package com.keeply.auth.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoUserInfoResponse {
  private Long id;

  @JsonProperty("kakao_account")
  private KakaoAccount kakaoAccount;

  @Getter
  @NoArgsConstructor
  public static class KakaoAccount {
    private Profile profile;

    @Getter
    @NoArgsConstructor
    public static class Profile {
      private String nickname;

      @JsonProperty("profile_image_url")
      private String profileImageUrl;
    }
  }

  public String getNickname() {
    if (kakaoAccount == null || kakaoAccount.getProfile() == null) {
      return null;
    }
    return kakaoAccount.getProfile().getNickname();
  }

  public String getProfileImageUrl() {
    if (kakaoAccount == null || kakaoAccount.getProfile() == null) {
      return null;
    }
    return kakaoAccount.getProfile().getProfileImageUrl();
  }
}
