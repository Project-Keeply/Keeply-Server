package com.keeply.common.security.aspect;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.repository.GroupMemberRepository;
import java.lang.reflect.Method;
import java.util.Optional;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@ExtendWith(MockitoExtension.class)
class GroupAccessAspectTest {

  private static final Long USER_ID = 1L;
  private static final Long GROUP_ID = 100L;

  @Mock private GroupMemberRepository groupMemberRepository;
  @Mock private JoinPoint joinPoint;
  @Mock private MethodSignature signature;

  private GroupAccessAspect aspect;

  @BeforeEach
  void setUp() {
    aspect = new GroupAccessAspect(groupMemberRepository);
  }

  static class DummyController {
    public void withGroupId(
        @AuthenticationPrincipal Long userId, @PathVariable("groupId") Long groupId) {}

    public void withImplicitGroupId(
        @AuthenticationPrincipal Long userId, @PathVariable Long groupId) {}

    public void withoutGroupId(@AuthenticationPrincipal Long userId) {}
  }

  private void mockJoinPointWithGroupId() throws NoSuchMethodException {
    Method method = DummyController.class.getMethod("withGroupId", Long.class, Long.class);
    given(joinPoint.getSignature()).willReturn(signature);
    given(signature.getMethod()).willReturn(method);
    given(joinPoint.getArgs()).willReturn(new Object[] {USER_ID, GROUP_ID});
  }

  private GroupMember memberOf(GroupRole role) {
    GroupMember member = Mockito.mock(GroupMember.class);
    Mockito.doReturn(role).when(member).getRole();
    return member;
  }

  @Nested
  @DisplayName("@GroupMemberOnly")
  class MemberOnly {
    @Test
    @DisplayName("소속 멤버 존재 → 통과 (role 무관)")
    void memberExistsPasses() throws Exception {
      mockJoinPointWithGroupId();
      given(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).willReturn(true);

      assertThatCode(() -> aspect.checkGroupMember(joinPoint)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("소속 없음 → NOT_GROUP_MEMBER")
    void notFoundThrows() throws Exception {
      mockJoinPointWithGroupId();
      given(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).willReturn(false);

      assertThatThrownBy(() -> aspect.checkGroupMember(joinPoint))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_GROUP_MEMBER);
    }
  }

  @Nested
  @DisplayName("@GroupOwnerOnly")
  class OwnerOnly {
    @Test
    @DisplayName("OWNER 멤버 → 통과")
    void ownerPasses() throws Exception {
      mockJoinPointWithGroupId();
      GroupMember member = memberOf(GroupRole.OWNER);
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID))
          .willReturn(Optional.of(member));

      assertThatCode(() -> aspect.checkGroupOwner(joinPoint)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("STAFF 멤버 → NOT_GROUP_OWNER")
    void staffThrows() throws Exception {
      mockJoinPointWithGroupId();
      GroupMember member = memberOf(GroupRole.STAFF);
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID))
          .willReturn(Optional.of(member));

      assertThatThrownBy(() -> aspect.checkGroupOwner(joinPoint))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_GROUP_OWNER);
    }

    @Test
    @DisplayName("소속 없음 → NOT_GROUP_MEMBER")
    void notFoundThrows() throws Exception {
      mockJoinPointWithGroupId();
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID))
          .willReturn(Optional.empty());

      assertThatThrownBy(() -> aspect.checkGroupOwner(joinPoint))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_GROUP_MEMBER);
    }
  }

  @Test
  @DisplayName("groupId 파라미터 없음 → IllegalStateException")
  void missingGroupIdThrows() throws Exception {
    Method method = DummyController.class.getMethod("withoutGroupId", Long.class);
    given(joinPoint.getSignature()).willReturn(signature);
    given(signature.getMethod()).willReturn(method);
    given(joinPoint.getArgs()).willReturn(new Object[] {USER_ID});

    assertThatThrownBy(() -> aspect.checkGroupMember(joinPoint))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("groupId");
  }

  @Test
  @DisplayName("@PathVariable Long groupId (value 생략) → parameter 이름 fallback으로 인식")
  void implicitPathVariableNameResolved() throws Exception {
    Method method = DummyController.class.getMethod("withImplicitGroupId", Long.class, Long.class);
    given(joinPoint.getSignature()).willReturn(signature);
    given(signature.getMethod()).willReturn(method);
    given(joinPoint.getArgs()).willReturn(new Object[] {USER_ID, GROUP_ID});
    given(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).willReturn(true);

    assertThatCode(() -> aspect.checkGroupMember(joinPoint)).doesNotThrowAnyException();
  }
}
