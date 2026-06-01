package com.keeply.onboarding.service;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.group.entity.Group;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.repository.GroupMemberRepository;
import com.keeply.group.repository.GroupRepository;
import com.keeply.onboarding.dto.request.OwnerOnboardingRequest;
import com.keeply.onboarding.dto.request.StaffOnboardingRequest;
import com.keeply.onboarding.dto.response.OwnerOnboardingResponse;
import com.keeply.onboarding.dto.response.StaffOnboardingResponse;
import com.keeply.onboarding.util.InviteCodeGenerator;
import com.keeply.user.entity.User;
import com.keeply.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

  private final UserRepository userRepository;
  private final GroupRepository groupRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final InviteCodeGenerator inviteCodeGenerator;

  @Override
  @Transactional
  public OwnerOnboardingResponse onboardOwner(Long userId, OwnerOnboardingRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    if (groupMemberRepository.existsByUserId(userId)) {
      throw new CustomException(ErrorCode.USER_ALREADY_IN_GROUP);
    }

    String inviteCode = inviteCodeGenerator.generateUniqueInviteCode();

    Group group =
        Group.builder()
            .name(request.getStoreName())
            .storeBrand(request.getStoreBrand())
            .inviteCode(inviteCode)
            .build();
    Group savedGroup = groupRepository.save(group);

    GroupMember groupMember =
        GroupMember.builder().user(user).group(savedGroup).role(GroupRole.OWNER).build();
    groupMemberRepository.save(groupMember);

    user.updateName(request.getName());

    return OwnerOnboardingResponse.from(savedGroup);
  }

  @Override
  @Transactional
  public StaffOnboardingResponse onboardStaff(Long userId, StaffOnboardingRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    if (groupMemberRepository.existsByUserId(userId)) {
      throw new CustomException(ErrorCode.USER_ALREADY_IN_GROUP);
    }

    Group group =
        groupRepository
            .findByInviteCode(request.getInviteCode())
            .orElseThrow(() -> new CustomException(ErrorCode.INVITE_CODE_NOT_FOUND));

    GroupMember groupMember =
        GroupMember.builder().user(user).group(group).role(GroupRole.STAFF).build();
    groupMemberRepository.save(groupMember);

    user.updateName(request.getName());

    return StaffOnboardingResponse.from(group);
  }
}
