package com.keeply.common.security.aspect;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.repository.GroupMemberRepository;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

@Aspect
@Component
@RequiredArgsConstructor
public class GroupAccessAspect {
  private final GroupMemberRepository groupMemberRepository;

  @Before("@annotation(com.keeply.common.security.annotation.GroupMemberOnly)")
  public void checkGroupMember(JoinPoint joinPoint) {
    GroupAccessArgs args = extractArgs(joinPoint);
    if (!groupMemberRepository.existsByGroupIdAndUserId(args.groupId(), args.userId())) {
      throw new CustomException(ErrorCode.NOT_GROUP_MEMBER);
    }
  }

  @Before("@annotation(com.keeply.common.security.annotation.GroupOwnerOnly)")
  public void checkGroupOwner(JoinPoint joinPoint) {
    GroupAccessArgs args = extractArgs(joinPoint);
    GroupMember member =
        groupMemberRepository
            .findByGroupIdAndUserId(args.groupId(), args.userId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
    if (member.getRole() != GroupRole.OWNER) {
      throw new CustomException(ErrorCode.NOT_GROUP_OWNER);
    }
  }

  private GroupAccessArgs extractArgs(JoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    Parameter[] parameters = method.getParameters();
    Object[] values = joinPoint.getArgs();

    Long userId = null;
    Long groupId = null;

    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      Object value = values[i];
      if (value == null || !Long.class.equals(parameter.getType())) {
        continue;
      }
      if (userId == null && parameter.isAnnotationPresent(AuthenticationPrincipal.class)) {
        userId = (Long) value;
        continue;
      }
      if (groupId == null && hasGroupIdPathVariable(parameter)) {
        groupId = (Long) value;
      }
    }

    if (userId == null || groupId == null) {
      throw new IllegalStateException(
          "GroupAccessAspect: @AuthenticationPrincipal Long userId 또는 @PathVariable Long groupId 파라미터를 찾을 수 없습니다. method="
              + method.getDeclaringClass().getSimpleName()
              + "#"
              + method.getName());
    }
    return new GroupAccessArgs(userId, groupId);
  }

  private boolean hasGroupIdPathVariable(Parameter parameter) {
    PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
    if (pathVariable == null) {
      return false;
    }
    String name = resolvePathVariableName(pathVariable, parameter);
    return "groupId".equals(name);
  }

  private String resolvePathVariableName(PathVariable pathVariable, Parameter parameter) {
    if (!pathVariable.value().isEmpty()) {
      return pathVariable.value();
    }
    if (!pathVariable.name().isEmpty()) {
      return pathVariable.name();
    }
    return parameter.getName();
  }

  private record GroupAccessArgs(Long userId, Long groupId) {}
}
