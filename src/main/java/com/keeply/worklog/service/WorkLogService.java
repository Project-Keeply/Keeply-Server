package com.keeply.worklog.service;

import com.keeply.common.response.PageResponse;
import com.keeply.worklog.dto.request.CreateWorkLogRequest;
import com.keeply.worklog.dto.request.UpdateWorkLogRequest;
import com.keeply.worklog.dto.response.WorkLogResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

public interface WorkLogService {

  WorkLogResponse createWorkLog(Long userId, Long groupId, CreateWorkLogRequest request);

  PageResponse<WorkLogResponse> getWorkLogList(
      Long groupId, LocalDate from, LocalDate to, Pageable pageable);

  WorkLogResponse getWorkLog(Long groupId, Long workLogId);

  WorkLogResponse updateWorkLog(
      Long userId, Long groupId, Long workLogId, UpdateWorkLogRequest request);

  void deleteWorkLog(Long userId, Long groupId, Long workLogId);
}
