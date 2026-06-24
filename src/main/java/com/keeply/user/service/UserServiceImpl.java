package com.keeply.user.service;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.user.dto.request.UpdateUserRequest;
import com.keeply.user.dto.response.UserResponse;
import com.keeply.user.entity.User;
import com.keeply.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserResponse getMe(Long userId) {
    User user =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    return UserResponse.of(user);
  }

  @Override
  @Transactional
  public UserResponse updateName(Long userId, UpdateUserRequest request) {
    User user =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    user.updateName(request.getName());
    return UserResponse.of(user);
  }
}
