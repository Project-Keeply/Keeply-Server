package com.keeply.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 해당 그룹에 소속된 멤버(OWNER 포함)만 접근 가능한 컨트롤러 메서드임을 표시.
 *
 * <p>메서드 시그니처에 {@code @AuthenticationPrincipal Long userId} 와 {@code @PathVariable Long groupId}(또는
 * {@code @PathVariable("groupId")})가 있어야 함.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GroupMemberOnly {}
