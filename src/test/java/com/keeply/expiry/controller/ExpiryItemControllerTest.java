package com.keeply.expiry.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keeply.auth.jwt.JwtProvider;
import com.keeply.common.response.PageResponse;
import com.keeply.common.security.annotation.GroupMemberOnly;
import com.keeply.expiry.dto.response.ExpiryItemResponse;
import com.keeply.expiry.entity.ExpiryItemCategory;
import com.keeply.expiry.service.ExpiryItemService;
import com.keeply.user.repository.UserRepository;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExpiryItemController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExpiryItemControllerTest {

  private static final Long USER_ID = 1L;
  private static final Long GROUP_ID = 100L;
  private static final Long ITEM_ID = 10L;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ExpiryItemService expiryItemService;
  @MockitoBean private JwtProvider jwtProvider;
  @MockitoBean private UserRepository userRepository;
  @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("목록 조회는 withinDays와 기본 정렬을 서비스로 전달한다")
  void getExpiryItemListBindsWithinDaysAndDefaultSort() throws Exception {
    PageResponse<ExpiryItemResponse> response =
        PageResponse.of(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    given(expiryItemService.getExpiryItemList(eq(GROUP_ID), eq(3), any())).willReturn(response);

    mockMvc
        .perform(
            get("/groups/{groupId}/expiry-items", GROUP_ID)
                .with(authentication(new TestingAuthenticationToken(USER_ID, null)))
                .param("withinDays", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(expiryItemService).getExpiryItemList(eq(GROUP_ID), eq(3), pageableCaptor.capture());

    Sort sort = pageableCaptor.getValue().getSort();
    assertThat(sort.getOrderFor("expireDate").getDirection()).isEqualTo(Sort.Direction.ASC);
    assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  @DisplayName("상세 조회는 itemId를 서비스로 전달한다")
  void getExpiryItemBindsItemId() throws Exception {
    mockMvc
        .perform(
            get("/groups/{groupId}/expiry-items/{itemId}", GROUP_ID, ITEM_ID)
                .with(authentication(new TestingAuthenticationToken(USER_ID, null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(expiryItemService).getExpiryItem(GROUP_ID, ITEM_ID);
  }

  @Test
  @DisplayName("수정 요청은 userId/groupId/itemId를 서비스로 전달한다")
  void updateExpiryItemBindsPathVariables() throws Exception {
    setAuthenticationPrincipal(USER_ID);

    mockMvc
        .perform(
            patch("/groups/{groupId}/expiry-items/{itemId}", GROUP_ID, ITEM_ID)
                .with(authentication(new TestingAuthenticationToken(USER_ID, null)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"DAIRY\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(expiryItemService).updateExpiryItem(eq(USER_ID), eq(GROUP_ID), eq(ITEM_ID), any());
  }

  @Test
  @DisplayName("수정 요청에 수정 필드가 없으면 400을 반환한다")
  void updateExpiryItemRejectsEmptyRequest() throws Exception {
    mockMvc
        .perform(
            patch("/groups/{groupId}/expiry-items/{itemId}", GROUP_ID, ITEM_ID)
                .with(authentication(new TestingAuthenticationToken(USER_ID, null)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("수정할 항목이 최소 하나 이상 있어야 합니다."));

    verifyNoInteractions(expiryItemService);
  }

  @Test
  @DisplayName("삭제 요청은 userId/groupId/itemId를 서비스로 전달한다")
  void deleteExpiryItemBindsPathVariables() throws Exception {
    setAuthenticationPrincipal(USER_ID);

    mockMvc
        .perform(
            delete("/groups/{groupId}/expiry-items/{itemId}", GROUP_ID, ITEM_ID)
                .with(authentication(new TestingAuthenticationToken(USER_ID, null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(expiryItemService).deleteExpiryItem(USER_ID, GROUP_ID, ITEM_ID);
  }

  @Test
  @DisplayName("생성 요청 imageUrl이 blank이면 400을 반환한다")
  void createExpiryItemRejectsBlankImageUrl() throws Exception {
    mockMvc
        .perform(
            post("/groups/{groupId}/expiry-items", GROUP_ID)
                .with(authentication(new TestingAuthenticationToken(USER_ID, null)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "productName": "삼각김밥 참치마요",
                      "expireDate": "2026-07-10",
                      "category": "FF",
                      "imageUrl": " "
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("상품 이미지 URL은 필수입니다."));

    verifyNoInteractions(expiryItemService);
  }

  @Test
  @DisplayName("생성 요청 category가 없으면 400을 반환한다")
  void createExpiryItemRejectsMissingCategory() throws Exception {
    mockMvc
        .perform(
            post("/groups/{groupId}/expiry-items", GROUP_ID)
                .with(authentication(new TestingAuthenticationToken(USER_ID, null)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "productName": "삼각김밥 참치마요",
                      "expireDate": "2026-07-10",
                      "imageUrl": "https://example.com/item.png"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("상품 카테고리는 필수입니다."));

    verifyNoInteractions(expiryItemService);
  }

  @Test
  @DisplayName("수정 요청 category만 있어도 서비스를 호출한다")
  void updateExpiryItemAcceptsCategoryOnly() throws Exception {
    setAuthenticationPrincipal(USER_ID);

    mockMvc
        .perform(
            patch("/groups/{groupId}/expiry-items/{itemId}", GROUP_ID, ITEM_ID)
                .with(authentication(new TestingAuthenticationToken(USER_ID, null)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"%s\"}".formatted(ExpiryItemCategory.DAIRY)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(expiryItemService).updateExpiryItem(eq(USER_ID), eq(GROUP_ID), eq(ITEM_ID), any());
  }

  @Test
  @DisplayName("모든 API 메서드에 GroupMemberOnly가 적용되어 있다")
  void everyEndpointRequiresGroupMember() {
    assertHasGroupMemberOnly("createExpiryItem");
    assertHasGroupMemberOnly("getExpiryItemList");
    assertHasGroupMemberOnly("getExpiryItem");
    assertHasGroupMemberOnly("updateExpiryItem");
    assertHasGroupMemberOnly("deleteExpiryItem");
  }

  private static void assertHasGroupMemberOnly(String methodName) {
    Method method =
        List.of(ExpiryItemController.class.getDeclaredMethods()).stream()
            .filter(candidate -> candidate.getName().equals(methodName))
            .findFirst()
            .orElseThrow();
    assertThat(method.isAnnotationPresent(GroupMemberOnly.class)).isTrue();
  }

  private static void setAuthenticationPrincipal(Long userId) {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(userId, null, List.of()));
  }
}
