package com.keeply.group.service;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.group.dto.response.GroupMemberResponse;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.repository.GroupMemberRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupMemberServiceImpl implements GroupMemberService {

  private final GroupMemberRepository groupMemberRepository;

  @Override
  @Transactional(readOnly = true)
  public List<GroupMemberResponse> getList(Long userId, Long groupId) {
    if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
      throw new CustomException(ErrorCode.NOT_GROUP_MEMBER);
    }

    return groupMemberRepository.findByGroupId(groupId).stream()
        .sorted(
            Comparator.comparing((GroupMember m) -> m.getRole() != GroupRole.OWNER)
                .thenComparing(GroupMember::getJoinedAt))
        .map(GroupMemberResponse::of)
        .toList();
  }

  @Override
  @Transactional
  public void leave(Long userId, Long groupId) {
    GroupMember groupMember =
        groupMemberRepository
            .findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));

    if (groupMember.getRole() == GroupRole.OWNER) {
      throw new CustomException(ErrorCode.OWNER_CANNOT_LEAVE);
    }
    groupMemberRepository.delete(groupMember);
  }
}
