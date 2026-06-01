package com.keeply.onboarding.service;

import com.keeply.onboarding.dto.request.OwnerOnboardingRequest;
import com.keeply.onboarding.dto.response.OwnerOnboardingResponse;

public interface OnboardingService {
  OwnerOnboardingResponse onboardOwner(Long userId, OwnerOnboardingRequest request);
}
