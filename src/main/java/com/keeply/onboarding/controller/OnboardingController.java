package com.keeply.onboarding.controller;

import com.keeply.common.response.ApiResponse;
import com.keeply.onboarding.dto.request.OwnerOnboardingRequest;
import com.keeply.onboarding.dto.response.OwnerOnboardingResponse;
import com.keeply.onboarding.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/onboarding")
public class OnboardingController {

  private final OnboardingService onboardingService;

  @PostMapping("/owner")
  public ApiResponse<OwnerOnboardingResponse> onboardOwner(
      @AuthenticationPrincipal Long userId, @RequestBody @Valid OwnerOnboardingRequest request) {
    OwnerOnboardingResponse response = onboardingService.onboardOwner(userId, request);
    return ApiResponse.success(response);
  }
}
