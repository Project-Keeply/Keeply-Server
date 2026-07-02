package com.keeply.notice.service;

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
import com.keeply.notice.dto.request.CreateNoticeRequest;
import com.keeply.notice.dto.request.UpdateNoticeRequest;
import com.keeply.notice.dto.response.NoticeListResponse;
import com.keeply.notice.dto.response.NoticeResponse;
import com.keeply.notice.entity.Notice;
import com.keeply.notice.entity.NoticeTag;
import com.keeply.notice.repository.NoticeRepository;
import com.keeply.user.entity.User;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Optional;
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
class NoticeServiceImplTest {

  private static final Long USER_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;
  private static final Long GROUP_ID = 100L;
  private static final Long NOTICE_ID = 10L;
  private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 7, 2, 10, 30);

  @Mock private NoticeRepository noticeRepository;
  @Mock private GroupMemberRepository groupMemberRepository;

  @InjectMocks private NoticeServiceImpl noticeService;

  @Nested
  @DisplayName("createNotice")
  class CreateNotice {

    @Test
    @DisplayName("그룹 멤버이면 공지를 생성한다")
    void createsNoticeWhenGroupMemberExists() {
      GroupMember authorMember = groupMember(USER_ID, "작성자", GroupRole.STAFF);
      CreateNoticeRequest request =
          createRequest("신상품 입고 안내", "신상품 진열 상태를 확인해주세요.", NoticeTag.DAILY, null);
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID))
          .willReturn(Optional.of(authorMember));
      given(noticeRepository.save(any(Notice.class)))
          .willAnswer(
              invocation -> {
                Notice notice = invocation.getArgument(0);
                setField(notice, "id", NOTICE_ID);
                setField(notice, "createdAt", CREATED_AT);
                return notice;
              });

      NoticeResponse response = noticeService.createNotice(USER_ID, GROUP_ID, request);

      assertThat(response.getNoticeId()).isEqualTo(NOTICE_ID);
      assertThat(response.getTitle()).isEqualTo("신상품 입고 안내");
      assertThat(response.getAuthorUserId()).isEqualTo(USER_ID);
      assertThat(response.getAuthorName()).isEqualTo("작성자");
    }
  }

  @Nested
  @DisplayName("getNoticeList")
  class GetNoticeList {

    @Test
    @DisplayName("tag가 없으면 그룹 기준 목록을 조회한다")
    void getsListByGroupWhenTagIsNull() {
      Notice notice = notice(NOTICE_ID, USER_ID, "작성자", GroupRole.STAFF, NoticeTag.DAILY);
      Pageable pageable = PageRequest.of(0, 10);
      given(noticeRepository.findByGroup_Id(GROUP_ID, pageable))
          .willReturn(new PageImpl<>(java.util.List.of(notice), pageable, 1));

      PageResponse<NoticeListResponse> response =
          noticeService.getNoticeList(GROUP_ID, null, pageable);

      assertThat(response.getContent()).hasSize(1);
      assertThat(response.getContent().get(0).getNoticeId()).isEqualTo(NOTICE_ID);
      assertThat(response.getPage()).isZero();
      assertThat(response.getSize()).isEqualTo(10);
      assertThat(response.getTotalElements()).isEqualTo(1);
      assertThat(response.getTotalPages()).isEqualTo(1);
      assertThat(response.isHasNext()).isFalse();
      verify(noticeRepository).findByGroup_Id(GROUP_ID, pageable);
      verify(noticeRepository, never()).findByGroup_IdAndTag(any(), any(), any());
    }

    @Test
    @DisplayName("tag가 있으면 그룹과 태그 기준 목록을 조회한다")
    void getsListByGroupAndTagWhenTagExists() {
      Notice notice = notice(NOTICE_ID, USER_ID, "작성자", GroupRole.STAFF, NoticeTag.WEEKLY);
      Pageable pageable = PageRequest.of(0, 10);
      given(noticeRepository.findByGroup_IdAndTag(GROUP_ID, NoticeTag.WEEKLY, pageable))
          .willReturn(new PageImpl<>(java.util.List.of(notice), pageable, 1));

      PageResponse<NoticeListResponse> response =
          noticeService.getNoticeList(GROUP_ID, NoticeTag.WEEKLY, pageable);

      assertThat(response.getContent()).hasSize(1);
      assertThat(response.getContent().get(0).getTag()).isEqualTo(NoticeTag.WEEKLY);
      verify(noticeRepository).findByGroup_IdAndTag(GROUP_ID, NoticeTag.WEEKLY, pageable);
      verify(noticeRepository, never()).findByGroup_Id(any(), any());
    }
  }

  @Nested
  @DisplayName("getNotice")
  class GetNotice {

    @Test
    @DisplayName("공지 상세를 조회한다")
    void getsNotice() {
      Notice notice = notice(NOTICE_ID, USER_ID, "작성자", GroupRole.STAFF, NoticeTag.DAILY);
      given(noticeRepository.findByIdAndGroup_Id(NOTICE_ID, GROUP_ID))
          .willReturn(Optional.of(notice));

      NoticeResponse response = noticeService.getNotice(GROUP_ID, NOTICE_ID);

      assertThat(response.getNoticeId()).isEqualTo(NOTICE_ID);
      assertThat(response.getContent()).isEqualTo("공지 내용");
    }

    @Test
    @DisplayName("공지 없음이면 NOTICE_NOT_FOUND 예외를 던진다")
    void throwsWhenNoticeNotFound() {
      given(noticeRepository.findByIdAndGroup_Id(NOTICE_ID, GROUP_ID)).willReturn(Optional.empty());

      assertThatThrownBy(() -> noticeService.getNotice(GROUP_ID, NOTICE_ID))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTICE_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("updateNotice")
  class UpdateNotice {

    @Test
    @DisplayName("작성자이면 공지를 부분 수정한다")
    void updatesNoticeWhenAuthor() {
      Notice notice = notice(NOTICE_ID, USER_ID, "작성자", GroupRole.STAFF, NoticeTag.DAILY);
      UpdateNoticeRequest request =
          updateRequest("수정 제목", "수정 내용", NoticeTag.WEEKLY, "https://example.com/image.png", null);
      given(noticeRepository.findByIdAndGroup_Id(NOTICE_ID, GROUP_ID))
          .willReturn(Optional.of(notice));

      NoticeResponse response = noticeService.updateNotice(USER_ID, GROUP_ID, NOTICE_ID, request);

      assertThat(response.getTitle()).isEqualTo("수정 제목");
      assertThat(response.getContent()).isEqualTo("수정 내용");
      assertThat(response.getTag()).isEqualTo(NoticeTag.WEEKLY);
      assertThat(response.getImageUrl()).isEqualTo("https://example.com/image.png");
    }

    @Test
    @DisplayName("작성자가 아니면 NOT_NOTICE_AUTHOR 예외를 던진다")
    void throwsWhenNotAuthor() {
      Notice notice = notice(NOTICE_ID, USER_ID, "작성자", GroupRole.STAFF, NoticeTag.DAILY);
      UpdateNoticeRequest request = updateRequest("수정 제목", null, null, null, null);
      given(noticeRepository.findByIdAndGroup_Id(NOTICE_ID, GROUP_ID))
          .willReturn(Optional.of(notice));

      assertThatThrownBy(
              () -> noticeService.updateNotice(OTHER_USER_ID, GROUP_ID, NOTICE_ID, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_NOTICE_AUTHOR);
    }

    @Test
    @DisplayName("수정 필드가 없으면 INVALID_INPUT 예외를 던진다")
    void throwsWhenNoUpdateField() {
      UpdateNoticeRequest request = updateRequest(null, null, null, null, null);

      assertThatThrownBy(() -> noticeService.updateNotice(USER_ID, GROUP_ID, NOTICE_ID, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

      verify(noticeRepository, never()).findByIdAndGroup_Id(any(), any());
    }

    @Test
    @DisplayName("removeImage가 false만 전달되면 INVALID_INPUT 예외를 던진다")
    void throwsWhenOnlyRemoveImageFalse() {
      UpdateNoticeRequest request = updateRequest(null, null, null, null, false);

      assertThatThrownBy(() -> noticeService.updateNotice(USER_ID, GROUP_ID, NOTICE_ID, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

      verify(noticeRepository, never()).findByIdAndGroup_Id(any(), any());
    }

    @Test
    @DisplayName("제목 또는 내용이 blank이면 INVALID_INPUT 예외를 던진다")
    void throwsWhenBlankFieldExists() {
      UpdateNoticeRequest request = updateRequest(" ", null, null, null, null);

      assertThatThrownBy(() -> noticeService.updateNotice(USER_ID, GROUP_ID, NOTICE_ID, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

      verify(noticeRepository, never()).findByIdAndGroup_Id(any(), any());
    }

    @Test
    @DisplayName("removeImage가 true이면 기존 이미지를 제거한다")
    void removesImageWhenRemoveImageIsTrue() {
      Notice notice =
          notice(
              NOTICE_ID,
              USER_ID,
              "작성자",
              GroupRole.STAFF,
              NoticeTag.DAILY,
              "https://example.com/image.png");
      UpdateNoticeRequest request = updateRequest(null, null, null, null, true);
      given(noticeRepository.findByIdAndGroup_Id(NOTICE_ID, GROUP_ID))
          .willReturn(Optional.of(notice));

      NoticeResponse response = noticeService.updateNotice(USER_ID, GROUP_ID, NOTICE_ID, request);

      assertThat(response.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("imageUrl과 removeImage=true가 동시에 전달되면 INVALID_INPUT 예외를 던진다")
    void throwsWhenImageUrlAndRemoveImageConflict() {
      UpdateNoticeRequest request =
          updateRequest(null, null, null, "https://example.com/image.png", true);

      assertThatThrownBy(() -> noticeService.updateNotice(USER_ID, GROUP_ID, NOTICE_ID, request))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

      verify(noticeRepository, never()).findByIdAndGroup_Id(any(), any());
    }
  }

  @Nested
  @DisplayName("deleteNotice")
  class DeleteNotice {

    @Test
    @DisplayName("작성자이면 공지를 삭제한다")
    void deletesNoticeWhenAuthor() {
      Notice notice = notice(NOTICE_ID, USER_ID, "작성자", GroupRole.STAFF, NoticeTag.DAILY);
      GroupMember authorMember = groupMember(USER_ID, "작성자", GroupRole.STAFF);
      given(noticeRepository.findByIdAndGroup_Id(NOTICE_ID, GROUP_ID))
          .willReturn(Optional.of(notice));
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID))
          .willReturn(Optional.of(authorMember));

      noticeService.deleteNotice(USER_ID, GROUP_ID, NOTICE_ID);

      verify(noticeRepository).delete(notice);
    }

    @Test
    @DisplayName("작성자가 아니어도 OWNER이면 공지를 삭제한다")
    void deletesNoticeWhenOwner() {
      Notice notice = notice(NOTICE_ID, USER_ID, "작성자", GroupRole.STAFF, NoticeTag.DAILY);
      GroupMember ownerMember = groupMember(OTHER_USER_ID, "점장", GroupRole.OWNER);
      given(noticeRepository.findByIdAndGroup_Id(NOTICE_ID, GROUP_ID))
          .willReturn(Optional.of(notice));
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OTHER_USER_ID))
          .willReturn(Optional.of(ownerMember));

      noticeService.deleteNotice(OTHER_USER_ID, GROUP_ID, NOTICE_ID);

      verify(noticeRepository).delete(notice);
    }

    @Test
    @DisplayName("작성자도 OWNER도 아니면 FORBIDDEN 예외를 던진다")
    void throwsWhenNotAuthorAndNotOwner() {
      Notice notice = notice(NOTICE_ID, USER_ID, "작성자", GroupRole.STAFF, NoticeTag.DAILY);
      GroupMember staffMember = groupMember(OTHER_USER_ID, "근무자", GroupRole.STAFF);
      given(noticeRepository.findByIdAndGroup_Id(NOTICE_ID, GROUP_ID))
          .willReturn(Optional.of(notice));
      given(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, OTHER_USER_ID))
          .willReturn(Optional.of(staffMember));

      assertThatThrownBy(() -> noticeService.deleteNotice(OTHER_USER_ID, GROUP_ID, NOTICE_ID))
          .isInstanceOf(CustomException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

      verify(noticeRepository, never()).delete(any());
    }
  }

  @Nested
  @DisplayName("display period")
  class DisplayPeriod {

    @Test
    @DisplayName("DAILY는 작성일 00:00부터 다음 날 00:00까지 표시한다")
    void dailyDisplayPeriod() {
      Notice notice = notice(NOTICE_ID, USER_ID, "작성자", GroupRole.STAFF, NoticeTag.DAILY);

      NoticeResponse response = NoticeResponse.of(notice);

      assertThat(response.getDisplayStartAt()).isEqualTo(LocalDateTime.of(2026, 7, 2, 0, 0));
      assertThat(response.getDisplayEndAt()).isEqualTo(LocalDateTime.of(2026, 7, 3, 0, 0));
    }

    @Test
    @DisplayName("WEEKLY는 작성 주 월요일 00:00부터 다음 주 월요일 00:00까지 표시한다")
    void weeklyDisplayPeriod() {
      Notice notice = notice(NOTICE_ID, USER_ID, "작성자", GroupRole.STAFF, NoticeTag.WEEKLY);

      NoticeResponse response = NoticeResponse.of(notice);

      assertThat(response.getDisplayStartAt()).isEqualTo(LocalDateTime.of(2026, 6, 29, 0, 0));
      assertThat(response.getDisplayEndAt()).isEqualTo(LocalDateTime.of(2026, 7, 6, 0, 0));
    }
  }

  private static CreateNoticeRequest createRequest(
      String title, String content, NoticeTag tag, String imageUrl) {
    CreateNoticeRequest request = createInstance(CreateNoticeRequest.class);
    setField(request, "title", title);
    setField(request, "content", content);
    setField(request, "tag", tag);
    setField(request, "imageUrl", imageUrl);
    return request;
  }

  private static UpdateNoticeRequest updateRequest(
      String title, String content, NoticeTag tag, String imageUrl, Boolean removeImage) {
    UpdateNoticeRequest request = createInstance(UpdateNoticeRequest.class);
    setField(request, "title", title);
    setField(request, "content", content);
    setField(request, "tag", tag);
    setField(request, "imageUrl", imageUrl);
    setField(request, "removeImage", removeImage);
    return request;
  }

  private static Notice notice(
      Long noticeId, Long authorUserId, String authorName, GroupRole authorRole, NoticeTag tag) {
    return notice(noticeId, authorUserId, authorName, authorRole, tag, null);
  }

  private static Notice notice(
      Long noticeId,
      Long authorUserId,
      String authorName,
      GroupRole authorRole,
      NoticeTag tag,
      String imageUrl) {
    Notice notice =
        Notice.builder()
            .authorMember(groupMember(authorUserId, authorName, authorRole))
            .title("공지 제목")
            .content("공지 내용")
            .tag(tag)
            .imageUrl(imageUrl)
            .build();
    setField(notice, "id", noticeId);
    setField(notice, "createdAt", CREATED_AT);
    return notice;
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
