package com.keeply.group.service;

import com.keeply.group.dto.request.UpdateGroupRequest;
import com.keeply.group.dto.response.GroupResponse;

public interface GroupService {

  GroupResponse getMyGroup(Long userId);

  GroupResponse updateMyGroup(Long userId, UpdateGroupRequest request);

  void deleteMyGroup(Long userId);

  GroupResponse reissueInviteCode(Long userId);

  void leaveMyGroup(Long userId);
}
