package com.keeply.expiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.common.response.PageResponse;
import com.keeply.expiry.dto.request.CreateExpiryItemRequest;
import com.keeply.expiry.dto.request.UpdateExpiryItemRequest;
import com.keeply.expiry.dto.response.ExpiryItemResponse;
import com.keeply.expiry.entity.ExpiryItem;
import com.keeply.expiry.entity.ExpiryItemCategory;
import com.keeply.expiry.repository.ExpiryItemRepository;
import com.keeply.file.service.FileService;
import com.keeply.group.entity.Group;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.repository.GroupMemberRepository;
import com.keeply.user.entity.User;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExpiryItemServiceImplTest {

  private static final Long USER_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;
  private static final Long GROUP_ID = 100L;
  private static final Long EXPIRY_ITEM_ID = 10L;
  private static final LocalDate EXPIRE_DATE = LocalDate.of(2026, 7, 10);
  private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 7, 2, 10, 30);
  private static final ExpiryItemCategory CATEGORY = ExpiryItemCategory.FF;
  private static final ExpiryItemCategory UPDATED_CATEGORY = ExpiryItemCategory.DAIRY;
  private static final String EXPIRY_ITEM_ACCESS_URL =
      "https://keeply-images.s3.ap-northeast-2.amazonaws.com/expiry-item/2026/07/item.png";
  private static final String PRESIGNED_EXPIRY_ITEM_URL =
      "https://keeply-images.s3.ap-northeast-2.amazonaws.com/expiry-item/2026/07/item.png?signature=fake";
  private static final String UPDATED_EXPIRY_ITEM_ACCESS_URL =
      "https://keeply-images.s3.ap-northeast-2.amazonaws.com/expiry-item/2026/07/new.png";
  private static final String UPDATED_PRESIGNED_EXPIRY_ITEM_URL =
      "https://keeply-images.s3.ap-northeast-2.amazonaws.com/expiry-item/2026/07/new.png?signature=fake";

  @Mock private ExpiryItemRepository expiryItemRepository;
  @Mock private GroupMemberRepository groupMemberRepository;
  @Mock private FileService fileService;

  @InjectMocks private ExpiryItemServiceImpl expiryItemService;

  @Nested
  @DisplayName("createExpiryItem")
  class CreateExpiryItem {

    @Test
    @DisplayName("그룹 멤버이면 유통기한 상품을 생성한다")
    void createsExpiryItemWhenGroupMemberExists() {
      GroupMember authorMember = groupMember(USER_ID, "작성자", GroupRole.STAFF);
      CreateExpiryItemRequest request =
          createRequest("삼각김밥 참치마요", EXPIRE_DATE, EXPIRY_ITEM_ACCESS_URL);
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID))
          .willReturn(Optional.of(authorMember));
      given(expiryItemRepository.save(any(ExpiryItem.class)))
          .willAnswer(
              invocation -> {
                ExpiryItem expiryItem = invocation.getArgument(0);
                setField(expiryItem, "id", EXPIRY_ITEM_ID);
                setField(expiryItem, "createdAt", CREATED_AT);
                return expiryItem;
              });
      given(fileService.getReadableUrl(EXPIRY_ITEM_ACCESS_URL))
          .willReturn(PRESIGNED_EXPIRY_ITEM_URL);

      ExpiryItemResponse response = expiryItemService.createExpiryItem(USER_ID, GROUP_ID, request);

      assertThat(response.getExpiryItemId()).isEqualTo(EXPIRY_ITEM_ID);
      assertThat(response.getProductName()).isEqualTo("삼각김밥 참치마요");
      assertThat(response.getExpireDate()).isEqualTo(EXPIRE_DATE);
      assertThat(response.getCategory()).isEqualTo(CATEGORY);
      assertThat(response.getImageUrl()).isEqualTo(PRESIGNED_EXPIRY_ITEM_URL);
      assertThat(response.getAuthorUserId()).isEqualTo(USER_ID);
      assertThat(response.getAuthorName()).isEqualTo("작성자");
      verify(fileService).getReadableUrl(EXPIRY_ITEM_ACCESS_URL);
    }

    @Test
    @DisplayName("그룹 멤버가 아니면 NOT_GROUP_MEMBER 예외를 던진다")
    void throwsWhenNotGroupMember() {
      CreateExpiryItemRequest request =
          createRequest("삼각김밥 참치마요", EXPIRE_DATE, "https://example.com/item.png");
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID))
          .willReturn(Optional.empty());

      assertThatThrownBy(() -> expiryItemService.createExpiryItem(USER_ID, GROUP_ID, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_GROUP_MEMBER);

      verify(expiryItemRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("getExpiryItemList")
  class GetExpiryItemList {

    @Test
    @DisplayName("withinDays가 없으면 그룹 기준 목록을 조회한다")
    void getsListByGroupWhenWithinDaysIsNull() {
      ExpiryItem expiryItem = expiryItem(EXPIRY_ITEM_ID, USER_ID, "작성자", GroupRole.STAFF);
      Pageable pageable = PageRequest.of(0, 10);
      given(expiryItemRepository.findByGroup_Id(GROUP_ID, pageable))
          .willReturn(new PageImpl<>(List.of(expiryItem), pageable, 1));
      given(fileService.getReadableUrl(EXPIRY_ITEM_ACCESS_URL))
          .willReturn(PRESIGNED_EXPIRY_ITEM_URL);

      PageResponse<ExpiryItemResponse> response =
          expiryItemService.getExpiryItemList(GROUP_ID, null, pageable);

      assertThat(response.getContent()).hasSize(1);
      assertThat(response.getContent().get(0).getExpiryItemId()).isEqualTo(EXPIRY_ITEM_ID);
      assertThat(response.getContent().get(0).getImageUrl()).isEqualTo(PRESIGNED_EXPIRY_ITEM_URL);
      assertThat(response.getPage()).isZero();
      assertThat(response.getSize()).isEqualTo(10);
      assertThat(response.getTotalElements()).isEqualTo(1);
      verify(expiryItemRepository).findByGroup_Id(GROUP_ID, pageable);
      verify(fileService).getReadableUrl(EXPIRY_ITEM_ACCESS_URL);
    }

    @Test
    @DisplayName("withinDays가 있으면 오늘부터 N일 이내 상품을 조회한다")
    void getsListByWithinDays() {
      ExpiryItem expiryItem = expiryItem(EXPIRY_ITEM_ID, USER_ID, "작성자", GroupRole.STAFF);
      Pageable pageable = PageRequest.of(0, 10);
      given(expiryItemRepository.findByGroup_IdAndExpireDateBetween(any(), any(), any(), any()))
          .willReturn(new PageImpl<>(List.of(expiryItem), pageable, 1));
      given(fileService.getReadableUrl(EXPIRY_ITEM_ACCESS_URL))
          .willReturn(PRESIGNED_EXPIRY_ITEM_URL);

      PageResponse<ExpiryItemResponse> response =
          expiryItemService.getExpiryItemList(GROUP_ID, 3, pageable);

      ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
      ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
      verify(expiryItemRepository)
          .findByGroup_IdAndExpireDateBetween(
              eq(GROUP_ID), fromCaptor.capture(), toCaptor.capture(), any());
      assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.now());
      assertThat(toCaptor.getValue()).isEqualTo(LocalDate.now().plusDays(3));
      assertThat(response.getContent().get(0).getImageUrl()).isEqualTo(PRESIGNED_EXPIRY_ITEM_URL);
      verify(expiryItemRepository, never()).findByGroup_Id(any(), any());
      verify(fileService).getReadableUrl(EXPIRY_ITEM_ACCESS_URL);
    }

    @Test
    @DisplayName("withinDays가 음수이면 INVALID_INPUT 예외를 던진다")
    void throwsWhenWithinDaysIsNegative() {
      Pageable pageable = PageRequest.of(0, 10);

      assertThatThrownBy(() -> expiryItemService.getExpiryItemList(GROUP_ID, -1, pageable))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

      verify(expiryItemRepository, never()).findByGroup_Id(any(), any());
    }
  }

  @Nested
  @DisplayName("getExpiryItem")
  class GetExpiryItem {

    @Test
    @DisplayName("유통기한 상품 상세를 조회한다")
    void getsExpiryItem() {
      ExpiryItem expiryItem = expiryItem(EXPIRY_ITEM_ID, USER_ID, "작성자", GroupRole.STAFF);
      given(expiryItemRepository.findByIdAndGroup_Id(EXPIRY_ITEM_ID, GROUP_ID))
          .willReturn(Optional.of(expiryItem));
      given(fileService.getReadableUrl(EXPIRY_ITEM_ACCESS_URL))
          .willReturn(PRESIGNED_EXPIRY_ITEM_URL);

      ExpiryItemResponse response = expiryItemService.getExpiryItem(GROUP_ID, EXPIRY_ITEM_ID);

      assertThat(response.getExpiryItemId()).isEqualTo(EXPIRY_ITEM_ID);
      assertThat(response.getProductName()).isEqualTo("삼각김밥 참치마요");
      assertThat(response.getCategory()).isEqualTo(CATEGORY);
      assertThat(response.getImageUrl()).isEqualTo(PRESIGNED_EXPIRY_ITEM_URL);
      verify(fileService).getReadableUrl(EXPIRY_ITEM_ACCESS_URL);
    }

    @Test
    @DisplayName("유통기한 상품 없음이면 EXPIRY_ITEM_NOT_FOUND 예외를 던진다")
    void throwsWhenExpiryItemNotFound() {
      given(expiryItemRepository.findByIdAndGroup_Id(EXPIRY_ITEM_ID, GROUP_ID))
          .willReturn(Optional.empty());

      assertThatThrownBy(() -> expiryItemService.getExpiryItem(GROUP_ID, EXPIRY_ITEM_ID))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRY_ITEM_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("updateExpiryItem")
  class UpdateExpiryItem {

    @Test
    @DisplayName("작성자이면 유통기한 상품을 수정한다")
    void updatesExpiryItemWhenAuthor() {
      ExpiryItem expiryItem = expiryItem(EXPIRY_ITEM_ID, USER_ID, "작성자", GroupRole.STAFF);
      UpdateExpiryItemRequest request =
          updateRequest(
              "삼각김밥 전주비빔",
              LocalDate.of(2026, 7, 11),
              UPDATED_CATEGORY,
              UPDATED_EXPIRY_ITEM_ACCESS_URL);
      given(expiryItemRepository.findByIdAndGroup_Id(EXPIRY_ITEM_ID, GROUP_ID))
          .willReturn(Optional.of(expiryItem));
      given(fileService.getReadableUrl(UPDATED_EXPIRY_ITEM_ACCESS_URL))
          .willReturn(UPDATED_PRESIGNED_EXPIRY_ITEM_URL);

      ExpiryItemResponse response =
          expiryItemService.updateExpiryItem(USER_ID, GROUP_ID, EXPIRY_ITEM_ID, request);

      assertThat(response.getProductName()).isEqualTo("삼각김밥 전주비빔");
      assertThat(response.getExpireDate()).isEqualTo(LocalDate.of(2026, 7, 11));
      assertThat(response.getCategory()).isEqualTo(UPDATED_CATEGORY);
      assertThat(response.getImageUrl()).isEqualTo(UPDATED_PRESIGNED_EXPIRY_ITEM_URL);
      verify(fileService).getReadableUrl(UPDATED_EXPIRY_ITEM_ACCESS_URL);
    }

    @Test
    @DisplayName("작성자가 아니면 NOT_EXPIRY_ITEM_AUTHOR 예외를 던진다")
    void throwsWhenNotAuthor() {
      ExpiryItem expiryItem = expiryItem(EXPIRY_ITEM_ID, USER_ID, "작성자", GroupRole.STAFF);
      UpdateExpiryItemRequest request = updateRequest("삼각김밥 전주비빔", null, null);
      given(expiryItemRepository.findByIdAndGroup_Id(EXPIRY_ITEM_ID, GROUP_ID))
          .willReturn(Optional.of(expiryItem));

      assertThatThrownBy(
              () ->
                  expiryItemService.updateExpiryItem(
                      OTHER_USER_ID, GROUP_ID, EXPIRY_ITEM_ID, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_EXPIRY_ITEM_AUTHOR);

      verify(groupMemberRepository, never()).findByGroupIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("유통기한 상품 없음이면 EXPIRY_ITEM_NOT_FOUND 예외를 던진다")
    void throwsWhenExpiryItemNotFound() {
      UpdateExpiryItemRequest request = updateRequest("삼각김밥 전주비빔", null, null);
      given(expiryItemRepository.findByIdAndGroup_Id(EXPIRY_ITEM_ID, GROUP_ID))
          .willReturn(Optional.empty());

      assertThatThrownBy(
              () -> expiryItemService.updateExpiryItem(USER_ID, GROUP_ID, EXPIRY_ITEM_ID, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRY_ITEM_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("deleteExpiryItem")
  class DeleteExpiryItem {

    @Test
    @DisplayName("작성자이면 유통기한 상품을 삭제한다")
    void deletesExpiryItemWhenAuthor() {
      ExpiryItem expiryItem = expiryItem(EXPIRY_ITEM_ID, USER_ID, "작성자", GroupRole.STAFF);
      GroupMember authorMember = groupMember(USER_ID, "작성자", GroupRole.STAFF);
      given(expiryItemRepository.findByIdAndGroup_Id(EXPIRY_ITEM_ID, GROUP_ID))
          .willReturn(Optional.of(expiryItem));
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID))
          .willReturn(Optional.of(authorMember));

      expiryItemService.deleteExpiryItem(USER_ID, GROUP_ID, EXPIRY_ITEM_ID);

      verify(expiryItemRepository).delete(expiryItem);
    }

    @Test
    @DisplayName("작성자가 아니어도 OWNER이면 유통기한 상품을 삭제한다")
    void deletesExpiryItemWhenOwner() {
      ExpiryItem expiryItem = expiryItem(EXPIRY_ITEM_ID, USER_ID, "작성자", GroupRole.STAFF);
      GroupMember ownerMember = groupMember(OTHER_USER_ID, "점장", GroupRole.OWNER);
      given(expiryItemRepository.findByIdAndGroup_Id(EXPIRY_ITEM_ID, GROUP_ID))
          .willReturn(Optional.of(expiryItem));
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OTHER_USER_ID))
          .willReturn(Optional.of(ownerMember));

      expiryItemService.deleteExpiryItem(OTHER_USER_ID, GROUP_ID, EXPIRY_ITEM_ID);

      verify(expiryItemRepository).delete(expiryItem);
    }

    @Test
    @DisplayName("작성자도 OWNER도 아니면 FORBIDDEN 예외를 던진다")
    void throwsWhenNotAuthorAndNotOwner() {
      ExpiryItem expiryItem = expiryItem(EXPIRY_ITEM_ID, USER_ID, "작성자", GroupRole.STAFF);
      GroupMember staffMember = groupMember(OTHER_USER_ID, "근무자", GroupRole.STAFF);
      given(expiryItemRepository.findByIdAndGroup_Id(EXPIRY_ITEM_ID, GROUP_ID))
          .willReturn(Optional.of(expiryItem));
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OTHER_USER_ID))
          .willReturn(Optional.of(staffMember));

      assertThatThrownBy(
              () -> expiryItemService.deleteExpiryItem(OTHER_USER_ID, GROUP_ID, EXPIRY_ITEM_ID))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

      verify(expiryItemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("유통기한 상품 없음이면 EXPIRY_ITEM_NOT_FOUND 예외를 던진다")
    void throwsWhenExpiryItemNotFound() {
      given(expiryItemRepository.findByIdAndGroup_Id(EXPIRY_ITEM_ID, GROUP_ID))
          .willReturn(Optional.empty());

      assertThatThrownBy(
              () -> expiryItemService.deleteExpiryItem(USER_ID, GROUP_ID, EXPIRY_ITEM_ID))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRY_ITEM_NOT_FOUND);

      verify(groupMemberRepository, never()).findByGroupIdAndUserId(any(), any());
      verify(expiryItemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("삭제 요청자가 그룹 멤버가 아니면 NOT_GROUP_MEMBER 예외를 던진다")
    void throwsWhenRequesterIsNotGroupMember() {
      ExpiryItem expiryItem = expiryItem(EXPIRY_ITEM_ID, USER_ID, "작성자", GroupRole.STAFF);
      given(expiryItemRepository.findByIdAndGroup_Id(EXPIRY_ITEM_ID, GROUP_ID))
          .willReturn(Optional.of(expiryItem));
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OTHER_USER_ID))
          .willReturn(Optional.empty());

      assertThatThrownBy(
              () -> expiryItemService.deleteExpiryItem(OTHER_USER_ID, GROUP_ID, EXPIRY_ITEM_ID))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_GROUP_MEMBER);

      verify(expiryItemRepository, never()).delete(any());
    }
  }

  @Nested
  @DisplayName("CreateExpiryItemRequest 검증")
  class CreateExpiryItemRequestValidation {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("imageUrl이 blank이면 검증에 실패한다")
    void failsWhenImageUrlIsBlank() {
      CreateExpiryItemRequest request = createRequest("삼각김밥 참치마요", EXPIRE_DATE, " ");

      Set<ConstraintViolation<CreateExpiryItemRequest>> violations = validator.validate(request);

      assertThat(violations)
          .anyMatch(violation -> violation.getMessage().equals("상품 이미지 URL은 필수입니다."));
    }

    @Test
    @DisplayName("category가 null이면 검증에 실패한다")
    void failsWhenCategoryIsNull() {
      CreateExpiryItemRequest request =
          createRequest("삼각김밥 참치마요", EXPIRE_DATE, null, "https://example.com/item.png");

      Set<ConstraintViolation<CreateExpiryItemRequest>> violations = validator.validate(request);

      assertThat(violations)
          .anyMatch(violation -> violation.getMessage().equals("상품 카테고리는 필수입니다."));
    }

    @Test
    @DisplayName("유효한 생성 요청이면 검증을 통과한다")
    void passesWhenValidRequest() {
      CreateExpiryItemRequest request =
          createRequest("삼각김밥 참치마요", EXPIRE_DATE, "https://example.com/item.png");

      Set<ConstraintViolation<CreateExpiryItemRequest>> violations = validator.validate(request);

      assertThat(violations).isEmpty();
    }
  }

  @Nested
  @DisplayName("UpdateExpiryItemRequest 검증")
  class UpdateExpiryItemRequestValidation {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("수정 필드가 없으면 검증에 실패한다")
    void failsWhenNoUpdateField() {
      UpdateExpiryItemRequest request = updateRequest(null, null, null);

      Set<ConstraintViolation<UpdateExpiryItemRequest>> violations = validator.validate(request);

      assertThat(violations)
          .anyMatch(violation -> violation.getMessage().equals("수정할 항목이 최소 하나 이상 있어야 합니다."));
    }

    @Test
    @DisplayName("상품명이 blank이면 검증에 실패한다")
    void failsWhenProductNameBlank() {
      UpdateExpiryItemRequest request = updateRequest(" ", null, null);

      Set<ConstraintViolation<UpdateExpiryItemRequest>> violations = validator.validate(request);

      assertThat(violations)
          .anyMatch(violation -> violation.getMessage().equals("상품명/이미지 URL은 공백일 수 없습니다."));
    }

    @Test
    @DisplayName("유효한 수정 요청이면 검증을 통과한다")
    void passesWhenValidRequest() {
      UpdateExpiryItemRequest request = updateRequest("삼각김밥 전주비빔", null, null);

      Set<ConstraintViolation<UpdateExpiryItemRequest>> violations = validator.validate(request);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("카테고리만 있어도 유효한 수정 요청이다")
    void passesWhenOnlyCategoryIsPresent() {
      UpdateExpiryItemRequest request = updateRequest(null, null, UPDATED_CATEGORY, null);

      Set<ConstraintViolation<UpdateExpiryItemRequest>> violations = validator.validate(request);

      assertThat(violations).isEmpty();
    }
  }

  private static CreateExpiryItemRequest createRequest(
      String productName, LocalDate expireDate, String imageUrl) {
    return createRequest(productName, expireDate, CATEGORY, imageUrl);
  }

  private static CreateExpiryItemRequest createRequest(
      String productName, LocalDate expireDate, ExpiryItemCategory category, String imageUrl) {
    CreateExpiryItemRequest request = createInstance(CreateExpiryItemRequest.class);
    setField(request, "productName", productName);
    setField(request, "expireDate", expireDate);
    setField(request, "category", category);
    setField(request, "imageUrl", imageUrl);
    return request;
  }

  private static UpdateExpiryItemRequest updateRequest(
      String productName, LocalDate expireDate, String imageUrl) {
    return updateRequest(productName, expireDate, null, imageUrl);
  }

  private static UpdateExpiryItemRequest updateRequest(
      String productName, LocalDate expireDate, ExpiryItemCategory category, String imageUrl) {
    UpdateExpiryItemRequest request = createInstance(UpdateExpiryItemRequest.class);
    setField(request, "productName", productName);
    setField(request, "expireDate", expireDate);
    setField(request, "category", category);
    setField(request, "imageUrl", imageUrl);
    return request;
  }

  private static ExpiryItem expiryItem(
      Long expiryItemId, Long authorUserId, String authorName, GroupRole authorRole) {
    ExpiryItem expiryItem =
        ExpiryItem.builder()
            .authorMember(groupMember(authorUserId, authorName, authorRole))
            .productName("삼각김밥 참치마요")
            .expireDate(EXPIRE_DATE)
            .category(CATEGORY)
            .imageUrl(EXPIRY_ITEM_ACCESS_URL)
            .build();
    setField(expiryItem, "id", expiryItemId);
    setField(expiryItem, "createdAt", CREATED_AT);
    return expiryItem;
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
