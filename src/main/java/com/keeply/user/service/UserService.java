package com.keeply.user.service;

import com.keeply.user.dto.request.UpdateUserRequest;
import com.keeply.user.dto.response.UserResponse;

public interface UserService {
  UserResponse getMe(Long userId);

  UserResponse updateName(Long userId, UpdateUserRequest request);

  void delete(Long userId);
}
