package com.keeply.worklog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keeply.auth.jwt.JwtProvider;
import com.keeply.common.response.PageResponse;
import com.keeply.common.security.annotation.GroupMemberOnly;
import com.keeply.user.repository.UserRepository;
import com.keeply.worklog.dto.response.WorkLogResponse;
import com.keeply.worklog.service.WorkLogService;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkLogControllerTest {

  private static final Long USER_ID = 1L;
  private static final Long GROUP_ID = 100L;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private WorkLogService workLogService;
  @MockitoBean private JwtProvider jwtProvider;
  @MockitoBean private UserRepository userRepository;
  @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

  @Test
  @DisplayName("목록 조회는 from/to 날짜와 기본 정렬을 서비스로 전달한다")
  void getWorkLogListBindsDateFilterAndDefaultSort() throws Exception {
    PageResponse<WorkLogResponse> response =
        PageResponse.of(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    given(
            workLogService.getWorkLogList(
                eq(GROUP_ID), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), any()))
        .willReturn(response);

    mockMvc
        .perform(
            get("/groups/{groupId}/work-logs", GROUP_ID)
                .with(authentication(new TestingAuthenticationToken(USER_ID, null)))
                .param("from", "2026-07-01")
                .param("to", "2026-07-03"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(workLogService)
        .getWorkLogList(
            eq(GROUP_ID),
            eq(LocalDate.of(2026, 7, 1)),
            eq(LocalDate.of(2026, 7, 3)),
            pageableCaptor.capture());

    Sort sort = pageableCaptor.getValue().getSort();
    assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  @DisplayName("생성 요청 content가 blank이면 400을 반환한다")
  void createWorkLogRejectsBlankContent() throws Exception {
    mockMvc
        .perform(
            post("/groups/{groupId}/work-logs", GROUP_ID)
                .with(authentication(new TestingAuthenticationToken(USER_ID, null)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("운영 로그 내용은 필수입니다."));

    verifyNoInteractions(workLogService);
  }

  @Test
  @DisplayName("모든 API 메서드에 GroupMemberOnly가 적용되어 있다")
  void everyEndpointRequiresGroupMember() {
    assertHasGroupMemberOnly("createWorkLog");
    assertHasGroupMemberOnly("getWorkLogList");
    assertHasGroupMemberOnly("getWorkLog");
    assertHasGroupMemberOnly("updateWorkLog");
    assertHasGroupMemberOnly("deleteWorkLog");
  }

  private static void assertHasGroupMemberOnly(String methodName) {
    Method method =
        List.of(WorkLogController.class.getDeclaredMethods()).stream()
            .filter(candidate -> candidate.getName().equals(methodName))
            .findFirst()
            .orElseThrow();
    assertThat(method.isAnnotationPresent(GroupMemberOnly.class)).isTrue();
  }
}
