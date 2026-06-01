package com.keeply.group.service;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.group.dto.response.GroupResponse;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

  private final GroupMemberRepository groupMemberRepository;

  @Override
  @Transactional(readOnly = true)
  public GroupResponse getMyGroup(Long userId) {
    GroupMember groupMember =
        groupMemberRepository
            .findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_HAS_NO_GROUP));

    return GroupResponse.of(groupMember.getGroup(), groupMember.getRole());
  }
}
