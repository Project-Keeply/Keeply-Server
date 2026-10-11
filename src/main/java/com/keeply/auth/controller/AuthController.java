package com.keeply.auth.controller;

import com.keeply.auth.cookie.RefreshTokenCookieProvider;
import com.keeply.auth.dto.KakaoLoginRequest;
import com.keeply.auth.dto.LoginResponse;
import com.keeply.auth.dto.TokenIssueResult;
import com.keeply.auth.service.AuthService;
import com.keeply.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;
  private final RefreshTokenCookieProvider refreshTokenCookieProvider;

  @Operation(
      summary = "카카오 로그인",
      description = "Access Token은 JSON, Refresh Token은 HttpOnly 쿠키로 발급합니다.",
      parameters =
          @Parameter(
              name = "Origin",
              in = ParameterIn.HEADER,
              required = true,
              description = "서버가 정확히 허용한 클라이언트 Origin",
              schema = @Schema(type = "string")))
  @SecurityRequirements
  @PostMapping("/kakao/callback")
  public ApiResponse<LoginResponse> kakaoLogin(
      @RequestBody @Valid KakaoLoginRequest request, HttpServletResponse response) {
    TokenIssueResult result = authService.login(request.getCode());
    addRefreshCookie(response, result);
    return ApiResponse.success(LoginResponse.builder().accessToken(result.accessToken()).build());
  }

  @Operation(
      summary = "토큰 재발급",
      description = "Access Token 없이 Refresh 쿠키로 재발급하고 쿠키를 교체합니다. 실패 시 쿠키를 변경하지 않습니다.",
      parameters =
          @Parameter(
              name = "Origin",
              in = ParameterIn.HEADER,
              required = true,
              description = "서버가 정확히 허용한 클라이언트 Origin",
              schema = @Schema(type = "string")))
  @SecurityRequirements
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "Refresh 쿠키 누락, 만료, 무효 또는 최신 토큰 불일치",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "허용되지 않은 Origin",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "서버 처리 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  @PostMapping("/reissue")
  public ApiResponse<LoginResponse> reissue(
      @Parameter(
              name = RefreshTokenCookieProvider.NAME,
              in = ParameterIn.COOKIE,
              required = false,
              description = "로그인 또는 이전 재발급에서 받은 HttpOnly Refresh 쿠키")
          @CookieValue(name = RefreshTokenCookieProvider.NAME, required = false)
          String refreshToken,
      HttpServletResponse response) {
    TokenIssueResult result = authService.reissue(refreshToken);
    addRefreshCookie(response, result);
    return ApiResponse.success(LoginResponse.builder().accessToken(result.accessToken()).build());
  }

  @Operation(
      summary = "로그아웃",
      description = "Access Token 없이 Refresh 쿠키의 세션을 폐기하고 쿠키를 만료합니다. 누락·무효 쿠키도 성공 처리합니다.",
      parameters =
          @Parameter(
              name = "Origin",
              in = ParameterIn.HEADER,
              required = true,
              description = "서버가 정확히 허용한 클라이언트 Origin",
              schema = @Schema(type = "string")))
  @SecurityRequirements
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "허용되지 않은 Origin",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "서버 처리 실패; 쿠키를 삭제하지 않습니다.",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  @PostMapping("/logout")
  public ApiResponse<Void> logout(
      @Parameter(
              name = RefreshTokenCookieProvider.NAME,
              in = ParameterIn.COOKIE,
              required = false,
              description = "폐기할 로그인 세션의 HttpOnly Refresh 쿠키")
          @CookieValue(name = RefreshTokenCookieProvider.NAME, required = false)
          String refreshToken,
      HttpServletResponse response) {
    authService.logoutSession(refreshToken);
    response.addHeader(
        HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.createExpiredCookie().toString());
    return ApiResponse.success(null);
  }

  private void addRefreshCookie(HttpServletResponse response, TokenIssueResult result) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        refreshTokenCookieProvider
            .createCookie(result.refreshToken(), result.refreshTokenExpiresAt())
            .toString());
  }
}
