package com.keeply.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.keeply.auth.repository.RefreshTokenRepository;
import com.keeply.group.entity.Group;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.entity.StoreBrand;
import com.keeply.group.repository.GroupMemberRepository;
import com.keeply.user.dto.response.UserResponse;
import com.keeply.user.entity.User;
import com.keeply.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private GroupMemberRepository groupMemberRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks private UserServiceImpl userService;

  @Test
  @DisplayName("내 정보 조회 시 소속 그룹명이 있으면 함께 반환한다")
  void getMeReturnsGroupName() {
    Long userId = 1L;
    User user = createUser(userId);
    Group group = createGroup("CU 인하대점");
    GroupMember groupMember = createGroupMember(user, group);
    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
    given(groupMemberRepository.findByUserIdWithGroup(userId)).willReturn(Optional.of(groupMember));

    UserResponse response = userService.getMe(userId);

    assertThat(response.getId()).isEqualTo(userId);
    assertThat(response.getName()).isEqualTo("홍길동");
    assertThat(response.getProfileImageUrl()).isEqualTo("https://example.com/profile.png");
    assertThat(response.getGroupName()).isEqualTo("CU 인하대점");
  }

  @Test
  @DisplayName("내 정보 조회 시 소속 그룹이 없으면 그룹명은 null이다")
  void getMeReturnsNullGroupNameWithoutGroup() {
    Long userId = 1L;
    User user = createUser(userId);
    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
    given(groupMemberRepository.findByUserIdWithGroup(userId)).willReturn(Optional.empty());

    UserResponse response = userService.getMe(userId);

    assertThat(response.getGroupName()).isNull();
  }

  private User createUser(Long userId) {
    return User.builder()
        .id(userId)
        .kakaoId("kakao-1")
        .name("홍길동")
        .profileImageUrl("https://example.com/profile.png")
        .isNameCustomized(false)
        .build();
  }

  private Group createGroup(String name) {
    return Group.builder().id(1L).name(name).storeBrand(StoreBrand.CU).inviteCode("ABC123").build();
  }

  private GroupMember createGroupMember(User user, Group group) {
    return GroupMember.builder().id(1L).user(user).group(group).role(GroupRole.STAFF).build();
  }
}
