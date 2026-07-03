package com.keeply.worklog.service;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.common.response.PageResponse;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.repository.GroupMemberRepository;
import com.keeply.worklog.dto.request.CreateWorkLogRequest;
import com.keeply.worklog.dto.request.UpdateWorkLogRequest;
import com.keeply.worklog.dto.response.WorkLogResponse;
import com.keeply.worklog.entity.WorkLog;
import com.keeply.worklog.repository.WorkLogRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkLogServiceImpl implements WorkLogService {

  private final WorkLogRepository workLogRepository;
  private final GroupMemberRepository groupMemberRepository;

  @Override
  @Transactional
  public WorkLogResponse createWorkLog(Long userId, Long groupId, CreateWorkLogRequest request) {
    GroupMember authorMember = getGroupMember(groupId, userId);
    WorkLog workLog =
        WorkLog.builder().authorMember(authorMember).content(request.getContent()).build();
    WorkLog savedWorkLog = workLogRepository.save(workLog);
    return WorkLogResponse.of(savedWorkLog);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<WorkLogResponse> getWorkLogList(
      Long groupId, LocalDate from, LocalDate to, Pageable pageable) {
    checkDateRange(from, to);
    Page<WorkLog> workLogs = getWorkLogPage(groupId, from, to, pageable);
    return PageResponse.of(workLogs.map(WorkLogResponse::of));
  }

  @Override
  @Transactional(readOnly = true)
  public WorkLogResponse getWorkLog(Long groupId, Long workLogId) {
    WorkLog workLog = getWorkLogByIdAndGroupId(workLogId, groupId);
    return WorkLogResponse.of(workLog);
  }

  @Override
  @Transactional
  public WorkLogResponse updateWorkLog(
      Long userId, Long groupId, Long workLogId, UpdateWorkLogRequest request) {
    WorkLog workLog = getWorkLogByIdAndGroupId(workLogId, groupId);
    if (!workLog.isAuthor(userId)) {
      throw new CustomException(ErrorCode.NOT_WORK_LOG_AUTHOR);
    }
    workLog.updateContent(request.getContent());
    return WorkLogResponse.of(workLog);
  }

  @Override
  @Transactional
  public void deleteWorkLog(Long userId, Long groupId, Long workLogId) {
    WorkLog workLog = getWorkLogByIdAndGroupId(workLogId, groupId);
    GroupMember groupMember = getGroupMember(groupId, userId);
    if (!workLog.isAuthor(userId) && groupMember.getRole() != GroupRole.OWNER) {
      throw new CustomException(ErrorCode.FORBIDDEN);
    }
    workLogRepository.delete(workLog);
  }

  private Page<WorkLog> getWorkLogPage(
      Long groupId, LocalDate from, LocalDate to, Pageable pageable) {
    if (from == null && to == null) {
      return workLogRepository.findByGroup_Id(groupId, pageable);
    }
    if (from != null && to == null) {
      return workLogRepository.findByGroup_IdAndCreatedAtGreaterThanEqual(
          groupId, from.atStartOfDay(), pageable);
    }
    if (from == null) {
      return workLogRepository.findByGroup_IdAndCreatedAtLessThan(
          groupId, to.plusDays(1).atStartOfDay(), pageable);
    }
    return workLogRepository.findByGroup_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        groupId, from.atStartOfDay(), to.plusDays(1).atStartOfDay(), pageable);
  }

  private void checkDateRange(LocalDate from, LocalDate to) {
    if (from != null && to != null && from.isAfter(to)) {
      throw new CustomException(ErrorCode.INVALID_INPUT);
    }
  }

  private WorkLog getWorkLogByIdAndGroupId(Long workLogId, Long groupId) {
    return workLogRepository
        .findByIdAndGroup_Id(workLogId, groupId)
        .orElseThrow(() -> new CustomException(ErrorCode.WORK_LOG_NOT_FOUND));
  }

  private GroupMember getGroupMember(Long groupId, Long userId) {
    return groupMemberRepository
        .findByGroupIdAndUserId(groupId, userId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
  }
}
