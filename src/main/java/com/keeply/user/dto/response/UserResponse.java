package com.keeply.user.dto.response;

import com.keeply.user.entity.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserResponse {

  private final Long id;
  private final String name;
  private final String profileImageUrl;
  private final String groupName;

  public static UserResponse of(User user) {
    return of(user, null);
  }

  public static UserResponse of(User user, String groupName) {
    return UserResponse.builder()
        .id(user.getId())
        .name(user.getName())
        .profileImageUrl(user.getProfileImageUrl())
        .groupName(groupName)
        .build();
  }
}
