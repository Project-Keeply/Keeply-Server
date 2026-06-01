package com.keeply.group.service;

import com.keeply.group.dto.response.GroupResponse;

public interface GroupService {

  GroupResponse getMyGroup(Long userId);
}
