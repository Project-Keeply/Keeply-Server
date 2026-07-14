package com.keeply.worklog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.common.response.PageResponse;
import com.keeply.group.entity.Group;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.repository.GroupMemberRepository;
import com.keeply.user.entity.User;
import com.keeply.worklog.dto.request.CreateWorkLogRequest;
import com.keeply.worklog.dto.request.UpdateWorkLogRequest;
import com.keeply.worklog.dto.response.WorkLogResponse;
import com.keeply.worklog.entity.WorkLog;
import com.keeply.worklog.repository.WorkLogRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkLogServiceImplTest {

  private static final Long USER_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;
  private static final Long GROUP_ID = 100L;
  private static final Long WORK_LOG_ID = 10L;
  private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 7, 2, 10, 30);

  @Mock private WorkLogRepository workLogRepository;
  @Mock private GroupMemberRepository groupMemberRepository;

  @InjectMocks private WorkLogServiceImpl workLogService;

  @Nested
  @DisplayName("createWorkLog")
  class CreateWorkLog {

    @Test
    @DisplayName("그룹 멤버이면 운영 로그를 생성한다")
    void createsWorkLogWhenGroupMemberExists() {
      GroupMember authorMember = groupMember(USER_ID, "작성자", GroupRole.STAFF);
      CreateWorkLogRequest request = createRequest("냉장고 온도 점검 요청");
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID))
          .willReturn(Optional.of(authorMember));
      given(workLogRepository.save(any(WorkLog.class)))
          .willAnswer(
              invocation -> {
                WorkLog workLog = invocation.getArgument(0);
                setField(workLog, "id", WORK_LOG_ID);
                setField(workLog, "createdAt", CREATED_AT);
                return workLog;
              });

      WorkLogResponse response = workLogService.createWorkLog(USER_ID, GROUP_ID, request);

      assertThat(response.getWorkLogId()).isEqualTo(WORK_LOG_ID);
      assertThat(response.getContent()).isEqualTo("냉장고 온도 점검 요청");
      assertThat(response.getAuthorUserId()).isEqualTo(USER_ID);
      assertThat(response.getAuthorName()).isEqualTo("작성자");
    }

    @Test
    @DisplayName("그룹 멤버가 아니면 NOT_GROUP_MEMBER 예외를 던진다")
    void throwsWhenNotGroupMember() {
      CreateWorkLogRequest request = createRequest("냉장고 온도 점검 요청");
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID))
          .willReturn(Optional.empty());

      assertThatThrownBy(() -> workLogService.createWorkLog(USER_ID, GROUP_ID, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_GROUP_MEMBER);

      verify(workLogRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("getWorkLogList")
  class GetWorkLogList {

    @Test
    @DisplayName("기간 필터가 없으면 그룹 기준 목록을 조회한다")
    void getsListByGroupWhenDateFilterIsNull() {
      WorkLog workLog = workLog(WORK_LOG_ID, USER_ID, "작성자", GroupRole.STAFF);
      Pageable pageable = PageRequest.of(0, 10);
      given(workLogRepository.findByGroup_Id(GROUP_ID, pageable))
          .willReturn(new PageImpl<>(List.of(workLog), pageable, 1));

      PageResponse<WorkLogResponse> response =
          workLogService.getWorkLogList(GROUP_ID, null, null, pageable);

      assertThat(response.getContent()).hasSize(1);
      assertThat(response.getContent().get(0).getWorkLogId()).isEqualTo(WORK_LOG_ID);
      assertThat(response.getPage()).isZero();
      assertThat(response.getSize()).isEqualTo(10);
      assertThat(response.getTotalElements()).isEqualTo(1);
      verify(workLogRepository).findByGroup_Id(GROUP_ID, pageable);
    }

    @Test
    @DisplayName("from만 있으면 해당 날짜 00시 이후 목록을 조회한다")
    void getsListFromDateWhenOnlyFromExists() {
      Pageable pageable = PageRequest.of(0, 10);
      LocalDate from = LocalDate.of(2026, 7, 1);
      given(
              workLogRepository.findByGroup_IdAndCreatedAtGreaterThanEqual(
                  GROUP_ID, LocalDateTime.of(2026, 7, 1, 0, 0), pageable))
          .willReturn(new PageImpl<>(List.of(), pageable, 0));

      workLogService.getWorkLogList(GROUP_ID, from, null, pageable);

      verify(workLogRepository)
          .findByGroup_IdAndCreatedAtGreaterThanEqual(
              GROUP_ID, LocalDateTime.of(2026, 7, 1, 0, 0), pageable);
      verify(workLogRepository, never()).findByGroup_Id(any(), any());
    }

    @Test
    @DisplayName("to만 있으면 해당 날짜 다음 날 00시 이전 목록을 조회한다")
    void getsListToDateWhenOnlyToExists() {
      Pageable pageable = PageRequest.of(0, 10);
      LocalDate to = LocalDate.of(2026, 7, 3);
      given(
              workLogRepository.findByGroup_IdAndCreatedAtLessThan(
                  GROUP_ID, LocalDateTime.of(2026, 7, 4, 0, 0), pageable))
          .willReturn(new PageImpl<>(List.of(), pageable, 0));

      workLogService.getWorkLogList(GROUP_ID, null, to, pageable);

      verify(workLogRepository)
          .findByGroup_IdAndCreatedAtLessThan(
              GROUP_ID, LocalDateTime.of(2026, 7, 4, 0, 0), pageable);
    }

    @Test
    @DisplayName("from과 to가 있으면 to 날짜를 포함하는 기간 목록을 조회한다")
    void getsListBetweenDatesWhenBothDateFilterExist() {
      Pageable pageable = PageRequest.of(0, 10);
      LocalDate from = LocalDate.of(2026, 7, 1);
      LocalDate to = LocalDate.of(2026, 7, 3);
      given(
              workLogRepository.findByGroup_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                  GROUP_ID,
                  LocalDateTime.of(2026, 7, 1, 0, 0),
                  LocalDateTime.of(2026, 7, 4, 0, 0),
                  pageable))
          .willReturn(new PageImpl<>(List.of(), pageable, 0));

      workLogService.getWorkLogList(GROUP_ID, from, to, pageable);

      verify(workLogRepository)
          .findByGroup_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
              GROUP_ID,
              LocalDateTime.of(2026, 7, 1, 0, 0),
              LocalDateTime.of(2026, 7, 4, 0, 0),
              pageable);
    }

    @Test
    @DisplayName("from이 to보다 늦으면 INVALID_INPUT 예외를 던진다")
    void throwsWhenFromIsAfterTo() {
      Pageable pageable = PageRequest.of(0, 10);
      LocalDate from = LocalDate.of(2026, 7, 4);
      LocalDate to = LocalDate.of(2026, 7, 3);

      assertThatThrownBy(() -> workLogService.getWorkLogList(GROUP_ID, from, to, pageable))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

      verify(workLogRepository, never()).findByGroup_Id(any(), any());
    }
  }

  @Nested
  @DisplayName("getWorkLog")
  class GetWorkLog {

    @Test
    @DisplayName("운영 로그 상세를 조회한다")
    void getsWorkLog() {
      WorkLog workLog = workLog(WORK_LOG_ID, USER_ID, "작성자", GroupRole.STAFF);
      given(workLogRepository.findByIdAndGroup_Id(WORK_LOG_ID, GROUP_ID))
          .willReturn(Optional.of(workLog));

      WorkLogResponse response = workLogService.getWorkLog(GROUP_ID, WORK_LOG_ID);

      assertThat(response.getWorkLogId()).isEqualTo(WORK_LOG_ID);
      assertThat(response.getContent()).isEqualTo("운영 로그 내용");
    }

    @Test
    @DisplayName("운영 로그 없음이면 WORK_LOG_NOT_FOUND 예외를 던진다")
    void throwsWhenWorkLogNotFound() {
      given(workLogRepository.findByIdAndGroup_Id(WORK_LOG_ID, GROUP_ID))
          .willReturn(Optional.empty());

      assertThatThrownBy(() -> workLogService.getWorkLog(GROUP_ID, WORK_LOG_ID))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORK_LOG_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("updateWorkLog")
  class UpdateWorkLog {

    @Test
    @DisplayName("작성자이면 운영 로그를 수정한다")
    void updatesWorkLogWhenAuthor() {
      WorkLog workLog = workLog(WORK_LOG_ID, USER_ID, "작성자", GroupRole.STAFF);
      UpdateWorkLogRequest request = updateRequest("수정된 운영 로그 내용");
      given(workLogRepository.findByIdAndGroup_Id(WORK_LOG_ID, GROUP_ID))
          .willReturn(Optional.of(workLog));

      WorkLogResponse response =
          workLogService.updateWorkLog(USER_ID, GROUP_ID, WORK_LOG_ID, request);

      assertThat(response.getContent()).isEqualTo("수정된 운영 로그 내용");
    }

    @Test
    @DisplayName("작성자가 아니면 NOT_WORK_LOG_AUTHOR 예외를 던진다")
    void throwsWhenNotAuthor() {
      WorkLog workLog = workLog(WORK_LOG_ID, USER_ID, "작성자", GroupRole.STAFF);
      UpdateWorkLogRequest request = updateRequest("수정된 운영 로그 내용");
      given(workLogRepository.findByIdAndGroup_Id(WORK_LOG_ID, GROUP_ID))
          .willReturn(Optional.of(workLog));

      assertThatThrownBy(
              () -> workLogService.updateWorkLog(OTHER_USER_ID, GROUP_ID, WORK_LOG_ID, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_WORK_LOG_AUTHOR);
    }

    @Test
    @DisplayName("비작성자이면 역할 조회 없이 NOT_WORK_LOG_AUTHOR 예외를 던진다")
    void throwsBeforeRoleLookupWhenNotAuthor() {
      WorkLog workLog = workLog(WORK_LOG_ID, USER_ID, "작성자", GroupRole.STAFF);
      UpdateWorkLogRequest request = updateRequest("수정된 운영 로그 내용");
      given(workLogRepository.findByIdAndGroup_Id(WORK_LOG_ID, GROUP_ID))
          .willReturn(Optional.of(workLog));

      assertThatThrownBy(
              () -> workLogService.updateWorkLog(OTHER_USER_ID, GROUP_ID, WORK_LOG_ID, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_WORK_LOG_AUTHOR);

      verify(groupMemberRepository, never()).findByGroupIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("운영 로그 없음이면 WORK_LOG_NOT_FOUND 예외를 던진다")
    void throwsWhenWorkLogNotFound() {
      UpdateWorkLogRequest request = updateRequest("수정된 운영 로그 내용");
      given(workLogRepository.findByIdAndGroup_Id(WORK_LOG_ID, GROUP_ID))
          .willReturn(Optional.empty());

      assertThatThrownBy(
              () -> workLogService.updateWorkLog(USER_ID, GROUP_ID, WORK_LOG_ID, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORK_LOG_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("deleteWorkLog")
  class DeleteWorkLog {

    @Test
    @DisplayName("작성자이면 운영 로그를 삭제한다")
    void deletesWorkLogWhenAuthor() {
      WorkLog workLog = workLog(WORK_LOG_ID, USER_ID, "작성자", GroupRole.STAFF);
      GroupMember authorMember = groupMember(USER_ID, "작성자", GroupRole.STAFF);
      given(workLogRepository.findByIdAndGroup_Id(WORK_LOG_ID, GROUP_ID))
          .willReturn(Optional.of(workLog));
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID))
          .willReturn(Optional.of(authorMember));

      workLogService.deleteWorkLog(USER_ID, GROUP_ID, WORK_LOG_ID);

      verify(workLogRepository).delete(workLog);
    }

    @Test
    @DisplayName("작성자가 아니어도 OWNER이면 운영 로그를 삭제한다")
    void deletesWorkLogWhenOwner() {
      WorkLog workLog = workLog(WORK_LOG_ID, USER_ID, "작성자", GroupRole.STAFF);
      GroupMember ownerMember = groupMember(OTHER_USER_ID, "점장", GroupRole.OWNER);
      given(workLogRepository.findByIdAndGroup_Id(WORK_LOG_ID, GROUP_ID))
          .willReturn(Optional.of(workLog));
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OTHER_USER_ID))
          .willReturn(Optional.of(ownerMember));

      workLogService.deleteWorkLog(OTHER_USER_ID, GROUP_ID, WORK_LOG_ID);

      verify(workLogRepository).delete(workLog);
    }

    @Test
    @DisplayName("작성자도 OWNER도 아니면 FORBIDDEN 예외를 던진다")
    void throwsWhenNotAuthorAndNotOwner() {
      WorkLog workLog = workLog(WORK_LOG_ID, USER_ID, "작성자", GroupRole.STAFF);
      GroupMember staffMember = groupMember(OTHER_USER_ID, "근무자", GroupRole.STAFF);
      given(workLogRepository.findByIdAndGroup_Id(WORK_LOG_ID, GROUP_ID))
          .willReturn(Optional.of(workLog));
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OTHER_USER_ID))
          .willReturn(Optional.of(staffMember));

      assertThatThrownBy(() -> workLogService.deleteWorkLog(OTHER_USER_ID, GROUP_ID, WORK_LOG_ID))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

      verify(workLogRepository, never()).delete(any());
    }

    @Test
    @DisplayName("운영 로그 없음이면 WORK_LOG_NOT_FOUND 예외를 던진다")
    void throwsWhenWorkLogNotFound() {
      given(workLogRepository.findByIdAndGroup_Id(WORK_LOG_ID, GROUP_ID))
          .willReturn(Optional.empty());

      assertThatThrownBy(() -> workLogService.deleteWorkLog(USER_ID, GROUP_ID, WORK_LOG_ID))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORK_LOG_NOT_FOUND);

      verify(groupMemberRepository, never()).findByGroupIdAndUserId(any(), any());
      verify(workLogRepository, never()).delete(any());
    }

    @Test
    @DisplayName("삭제 요청자가 그룹 멤버가 아니면 NOT_GROUP_MEMBER 예외를 던진다")
    void throwsWhenRequesterIsNotGroupMember() {
      WorkLog workLog = workLog(WORK_LOG_ID, USER_ID, "작성자", GroupRole.STAFF);
      given(workLogRepository.findByIdAndGroup_Id(WORK_LOG_ID, GROUP_ID))
          .willReturn(Optional.of(workLog));
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OTHER_USER_ID))
          .willReturn(Optional.empty());

      assertThatThrownBy(() -> workLogService.deleteWorkLog(OTHER_USER_ID, GROUP_ID, WORK_LOG_ID))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_GROUP_MEMBER);

      verify(workLogRepository, never()).delete(any());
    }
  }

  @Nested
  @DisplayName("CreateWorkLogRequest 검증")
  class CreateWorkLogRequestValidation {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("content가 null이면 검증에 실패한다")
    void failsWhenContentIsNull() {
      CreateWorkLogRequest request = createRequest(null);

      Set<ConstraintViolation<CreateWorkLogRequest>> violations = validator.validate(request);

      assertThat(violations)
          .anyMatch(violation -> violation.getMessage().equals("운영 로그 내용은 필수입니다."));
    }

    @Test
    @DisplayName("content가 빈 문자열이면 검증에 실패한다")
    void failsWhenContentIsEmpty() {
      CreateWorkLogRequest request = createRequest("");

      Set<ConstraintViolation<CreateWorkLogRequest>> violations = validator.validate(request);

      assertThat(violations)
          .anyMatch(violation -> violation.getMessage().equals("운영 로그 내용은 필수입니다."));
    }

    @Test
    @DisplayName("content가 blank이면 검증에 실패한다")
    void failsWhenContentIsBlank() {
      CreateWorkLogRequest request = createRequest(" ");

      Set<ConstraintViolation<CreateWorkLogRequest>> violations = validator.validate(request);

      assertThat(violations)
          .anyMatch(violation -> violation.getMessage().equals("운영 로그 내용은 필수입니다."));
    }

    @Test
    @DisplayName("유효한 생성 요청이면 검증을 통과한다")
    void passesWhenValidRequest() {
      CreateWorkLogRequest request = createRequest("냉장고 온도 점검 요청");

      Set<ConstraintViolation<CreateWorkLogRequest>> violations = validator.validate(request);

      assertThat(violations).isEmpty();
    }
  }

  @Nested
  @DisplayName("UpdateWorkLogRequest 검증")
  class UpdateWorkLogRequestValidation {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("수정 필드가 없으면 검증에 실패한다")
    void failsWhenNoUpdateField() {
      UpdateWorkLogRequest request = updateRequest(null);

      Set<ConstraintViolation<UpdateWorkLogRequest>> violations = validator.validate(request);

      assertThat(violations)
          .anyMatch(violation -> violation.getMessage().equals("수정할 항목이 최소 하나 이상 있어야 합니다."));
    }

    @Test
    @DisplayName("content가 blank이면 검증에 실패한다")
    void failsWhenContentBlank() {
      UpdateWorkLogRequest request = updateRequest(" ");

      Set<ConstraintViolation<UpdateWorkLogRequest>> violations = validator.validate(request);

      assertThat(violations)
          .anyMatch(violation -> violation.getMessage().equals("운영 로그 내용은 공백일 수 없습니다."));
    }

    @Test
    @DisplayName("유효한 수정 요청이면 검증을 통과한다")
    void passesWhenValidRequest() {
      UpdateWorkLogRequest request = updateRequest("수정된 운영 로그 내용");

      Set<ConstraintViolation<UpdateWorkLogRequest>> violations = validator.validate(request);

      assertThat(violations).isEmpty();
    }
  }

  private static CreateWorkLogRequest createRequest(String content) {
    CreateWorkLogRequest request = createInstance(CreateWorkLogRequest.class);
    setField(request, "content", content);
    return request;
  }

  private static UpdateWorkLogRequest updateRequest(String content) {
    UpdateWorkLogRequest request = createInstance(UpdateWorkLogRequest.class);
    setField(request, "content", content);
    return request;
  }

  private static WorkLog workLog(
      Long workLogId, Long authorUserId, String authorName, GroupRole authorRole) {
    WorkLog workLog =
        WorkLog.builder()
            .authorMember(groupMember(authorUserId, authorName, authorRole))
            .content("운영 로그 내용")
            .build();
    setField(workLog, "id", workLogId);
    setField(workLog, "createdAt", CREATED_AT);
    return workLog;
  }

  private static GroupMember groupMember(Long userId, String userName, GroupRole role) {
    GroupMember groupMember =
        GroupMember.builder().group(group()).user(user(userId, userName)).role(role).build();
    setField(groupMember, "id", userId);
    return groupMember;
  }

  private static Group group() {
    Group group = Group.builder().name("Keeply 편의점").build();
    setField(group, "id", GROUP_ID);
    return group;
  }

  private static User user(Long userId, String name) {
    User user = User.builder().kakaoId("kakao-" + userId).name(name).build();
    setField(user, "id", userId);
    return user;
  }

  private static void setField(Object target, String name, Object value) {
    ReflectionTestUtils.setField(target, name, value);
  }

  private static <T> T createInstance(Class<T> type) {
    try {
      Constructor<T> constructor = type.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
