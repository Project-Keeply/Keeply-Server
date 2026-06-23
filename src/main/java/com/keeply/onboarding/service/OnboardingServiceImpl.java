package com.keeply.onboarding.service;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.group.entity.Group;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.repository.GroupMemberRepository;
import com.keeply.group.repository.GroupRepository;
import com.keeply.group.util.InviteCodeGenerator;
import com.keeply.onboarding.dto.request.OwnerOnboardingRequest;
import com.keeply.onboarding.dto.request.StaffOnboardingRequest;
import com.keeply.onboarding.dto.response.OwnerOnboardingResponse;
import com.keeply.onboarding.dto.response.StaffOnboardingResponse;
import com.keeply.user.entity.User;
import com.keeply.user.repository.UserRepository;
import java.util.Optional;
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

    Optional<GroupMember> existing = groupMemberRepository.findByUserIdIncludingDeleted(userId);
    if (existing.isPresent() && existing.get().getDeletedAt() == null) {
      throw new CustomException(ErrorCode.USER_ALREADY_IN_GROUP);
    }

    Group savedGroup =
        inviteCodeGenerator.generateAndPersist(
            inviteCode -> {
              Group group =
                  Group.builder()
                      .name(request.getStoreName())
                      .storeBrand(request.getStoreBrand())
                      .inviteCode(inviteCode)
                      .build();
              return groupRepository.save(group);
            });

    if (existing.isPresent()) {
      existing.get().reactivate(savedGroup, GroupRole.OWNER);
    } else {
      groupMemberRepository.save(
          GroupMember.builder().user(user).group(savedGroup).role(GroupRole.OWNER).build());
    }

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

    Optional<GroupMember> existing = groupMemberRepository.findByUserIdIncludingDeleted(userId);
    if (existing.isPresent() && existing.get().getDeletedAt() == null) {
      throw new CustomException(ErrorCode.USER_ALREADY_IN_GROUP);
    }

    Group group =
        groupRepository
            .findByInviteCode(request.getInviteCode())
            .orElseThrow(() -> new CustomException(ErrorCode.INVITE_CODE_NOT_FOUND));

    if (existing.isPresent()) {
      existing.get().reactivate(group, GroupRole.STAFF);
    } else {
      groupMemberRepository.save(
          GroupMember.builder().user(user).group(group).role(GroupRole.STAFF).build());
    }

    user.updateName(request.getName());

    return StaffOnboardingResponse.from(group);
  }
}
