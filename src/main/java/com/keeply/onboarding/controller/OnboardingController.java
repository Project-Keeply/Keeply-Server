package com.keeply.onboarding.controller;

import com.keeply.common.response.ApiResponse;
import com.keeply.onboarding.dto.request.OwnerOnboardingRequest;
import com.keeply.onboarding.dto.request.StaffOnboardingRequest;
import com.keeply.onboarding.dto.response.OwnerOnboardingResponse;
import com.keeply.onboarding.dto.response.StaffOnboardingResponse;
import com.keeply.onboarding.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/onboarding")
public class OnboardingController {

  private final OnboardingService onboardingService;

  @PostMapping("/owner")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<OwnerOnboardingResponse> onboardOwner(
      @AuthenticationPrincipal Long userId, @RequestBody @Valid OwnerOnboardingRequest request) {
    OwnerOnboardingResponse response = onboardingService.onboardOwner(userId, request);
    return ApiResponse.success(response);
  }

  @PostMapping("/staff")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<StaffOnboardingResponse> onboardStaff(
      @AuthenticationPrincipal Long userId, @RequestBody @Valid StaffOnboardingRequest request) {
    StaffOnboardingResponse response = onboardingService.onboardStaff(userId, request);
    return ApiResponse.success(response);
  }
}
