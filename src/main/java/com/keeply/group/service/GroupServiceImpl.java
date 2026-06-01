package com.keeply.group.service;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.group.dto.request.UpdateGroupRequest;
import com.keeply.group.dto.response.GroupResponse;
import com.keeply.group.entity.Group;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.repository.GroupMemberRepository;
import com.keeply.group.repository.GroupRepository;
import com.keeply.onboarding.util.InviteCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

  private final GroupMemberRepository groupMemberRepository;
  private final GroupRepository groupRepository;
  private final InviteCodeGenerator inviteCodeGenerator;

  @Override
  @Transactional(readOnly = true)
  public GroupResponse getMyGroup(Long userId) {
    GroupMember groupMember =
        groupMemberRepository
            .findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_HAS_NO_GROUP));

    return GroupResponse.of(groupMember.getGroup(), groupMember.getRole());
  }

  @Override
  @Transactional
  public GroupResponse updateMyGroup(Long userId, UpdateGroupRequest request) {
    GroupMember groupMember =
        groupMemberRepository
            .findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_HAS_NO_GROUP));

    if (groupMember.getRole() != GroupRole.OWNER) {
      throw new CustomException(ErrorCode.NOT_GROUP_OWNER);
    }

    Group group = groupMember.getGroup();
    group.updateInfo(request.getName(), request.getStoreBrand());

    return GroupResponse.of(group, groupMember.getRole());
  }

  @Override
  @Transactional
  public void deleteMyGroup(Long userId) {
    GroupMember groupMember =
        groupMemberRepository
            .findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_HAS_NO_GROUP));

    if (groupMember.getRole() != GroupRole.OWNER) {
      throw new CustomException(ErrorCode.NOT_GROUP_OWNER);
    }

    Group group = groupMember.getGroup();
    groupMemberRepository.deleteByGroupId(group.getId());
    groupRepository.delete(group);
  }

  @Override
  @Transactional
  public GroupResponse reissueInviteCode(Long userId) {
    GroupMember groupMember =
        groupMemberRepository
            .findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_HAS_NO_GROUP));

    if (groupMember.getRole() != GroupRole.OWNER) {
      throw new CustomException(ErrorCode.NOT_GROUP_OWNER);
    }

    String newInviteCode = inviteCodeGenerator.generateUniqueInviteCode();
    Group group = groupMember.getGroup();
    group.updateInviteCode(newInviteCode);

    return GroupResponse.of(group, groupMember.getRole());
  }

  @Override
  @Transactional
  public void leaveMyGroup(Long userId) {
    GroupMember groupMember =
        groupMemberRepository
            .findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_HAS_NO_GROUP));

    if (groupMember.getRole() == GroupRole.OWNER) {
      throw new CustomException(ErrorCode.OWNER_CANNOT_LEAVE);
    }

    groupMemberRepository.delete(groupMember);
  }
}
