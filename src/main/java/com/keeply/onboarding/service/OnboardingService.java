package com.keeply.onboarding.service;

import com.keeply.onboarding.dto.request.OwnerOnboardingRequest;
import com.keeply.onboarding.dto.request.StaffOnboardingRequest;
import com.keeply.onboarding.dto.response.OwnerOnboardingResponse;
import com.keeply.onboarding.dto.response.StaffOnboardingResponse;

public interface OnboardingService {
  OwnerOnboardingResponse onboardOwner(Long userId, OwnerOnboardingRequest request);

  StaffOnboardingResponse onboardStaff(Long userId, StaffOnboardingRequest request);
}
