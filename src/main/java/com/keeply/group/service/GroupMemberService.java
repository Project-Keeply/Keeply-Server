package com.keeply.group.service;

import com.keeply.group.dto.response.GroupMemberResponse;
import java.util.List;

public interface GroupMemberService {
  List<GroupMemberResponse> getList(Long userId, Long groupId);

  void leave(Long userId, Long groupId);
}
