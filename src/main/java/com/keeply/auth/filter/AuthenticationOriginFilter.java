package com.keeply.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keeply.common.config.CorsProperties;
import com.keeply.common.exception.ErrorCode;
import com.keeply.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

/** 쿠키 인증 POST의 CSRF 방어. SecurityFilterChain에서만 생성/등록한다. */
@RequiredArgsConstructor
public class AuthenticationOriginFilter extends OncePerRequestFilter {

  private static final List<String> COOKIE_AUTHENTICATION_PATHS =
      List.of("/auth/kakao/callback", "/auth/reissue", "/auth/logout");

  private final CorsProperties corsProperties;
  private final ObjectMapper objectMapper;

  public static boolean hasCookieAuthenticationRequest(HttpServletRequest request) {
    return HttpMethod.POST.matches(request.getMethod())
        && COOKIE_AUTHENTICATION_PATHS.contains(
            UrlPathHelper.defaultInstance.getPathWithinApplication(request));
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    return !hasCookieAuthenticationRequest(request);
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    List<String> origins = Collections.list(request.getHeaders(HttpHeaders.ORIGIN));
    if (origins.size() != 1
        || "null".equals(origins.getFirst())
        || !corsProperties.getAllowedOrigins().contains(origins.getFirst())) {
      ErrorCode errorCode = ErrorCode.AUTH_ORIGIN_NOT_ALLOWED;
      response.setStatus(errorCode.getHttpStatus().value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      objectMapper.writeValue(response.getWriter(), ApiResponse.failure(errorCode.getMessage()));
      return;
    }
    filterChain.doFilter(request, response);
  }
}
