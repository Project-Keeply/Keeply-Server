package com.keeply.notice.service;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.common.response.PageResponse;
import com.keeply.file.service.FileService;
import com.keeply.group.entity.GroupMember;
import com.keeply.group.entity.GroupRole;
import com.keeply.group.repository.GroupMemberRepository;
import com.keeply.notice.domain.NoticeDisplayPeriod;
import com.keeply.notice.dto.request.CreateNoticeRequest;
import com.keeply.notice.dto.request.UpdateNoticeRequest;
import com.keeply.notice.dto.response.NoticeListResponse;
import com.keeply.notice.dto.response.NoticeResponse;
import com.keeply.notice.entity.Notice;
import com.keeply.notice.entity.NoticeTag;
import com.keeply.notice.repository.NoticeRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

  private final NoticeRepository noticeRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final FileService fileService;
  private final Clock clock;

  @Override
  @Transactional
  public NoticeResponse createNotice(Long userId, Long groupId, CreateNoticeRequest request) {
    GroupMember authorMember = getGroupMember(groupId, userId);
    Notice notice =
        Notice.builder()
            .authorMember(authorMember)
            .title(request.getTitle())
            .content(request.getContent())
            .tag(request.getTag())
            .imageUrl(request.getImageUrl())
            .build();
    Notice savedNotice = noticeRepository.save(notice);
    return toNoticeResponse(savedNotice);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<NoticeListResponse> getNoticeList(
      Long groupId, NoticeTag tag, Pageable pageable) {
    return getNoticeList(groupId, tag, false, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<NoticeListResponse> getNoticeList(
      Long groupId, NoticeTag tag, boolean isActive, Pageable pageable) {
    Page<Notice> notices =
        isActive
            ? getActiveNoticePage(groupId, tag, pageable)
            : getNoticePage(groupId, tag, pageable);
    return PageResponse.of(notices.map(this::toNoticeListResponse));
  }

  @Override
  @Transactional(readOnly = true)
  public NoticeResponse getNotice(Long groupId, Long noticeId) {
    Notice notice = getNoticeByIdAndGroupId(noticeId, groupId);
    return toNoticeResponse(notice);
  }

  @Override
  @Transactional
  public NoticeResponse updateNotice(
      Long userId, Long groupId, Long noticeId, UpdateNoticeRequest request) {
    Notice notice = getNoticeByIdAndGroupId(noticeId, groupId);
    if (!notice.isAuthor(userId)) {
      throw new CustomException(ErrorCode.NOT_NOTICE_AUTHOR);
    }
    notice.updateInfo(
        request.getTitle(),
        request.getContent(),
        request.getTag(),
        request.getImageUrl(),
        request.isRemoveImage());
    return toNoticeResponse(notice);
  }

  @Override
  @Transactional
  public void deleteNotice(Long userId, Long groupId, Long noticeId) {
    Notice notice = getNoticeByIdAndGroupId(noticeId, groupId);
    GroupMember groupMember = getGroupMember(groupId, userId);
    if (!notice.isAuthor(userId) && groupMember.getRole() != GroupRole.OWNER) {
      throw new CustomException(ErrorCode.FORBIDDEN);
    }
    noticeRepository.delete(notice);
  }

  private Notice getNoticeByIdAndGroupId(Long noticeId, Long groupId) {
    return noticeRepository
        .findByIdAndGroup_Id(noticeId, groupId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));
  }

  private Page<Notice> getNoticePage(Long groupId, NoticeTag tag, Pageable pageable) {
    return tag == null
        ? noticeRepository.findByGroup_Id(groupId, pageable)
        : noticeRepository.findByGroup_IdAndTag(groupId, tag, pageable);
  }

  private Page<Notice> getActiveNoticePage(Long groupId, NoticeTag tag, Pageable pageable) {
    LocalDate today = LocalDate.now(clock);
    NoticeDisplayPeriod dailyDisplayPeriod = NoticeDisplayPeriod.from(NoticeTag.DAILY, today);
    NoticeDisplayPeriod weeklyDisplayPeriod = NoticeDisplayPeriod.from(NoticeTag.WEEKLY, today);

    if (tag == null) {
      return noticeRepository.findActiveByGroup_Id(
          groupId,
          NoticeTag.DAILY,
          dailyDisplayPeriod.startAt(),
          dailyDisplayPeriod.endAt(),
          NoticeTag.WEEKLY,
          weeklyDisplayPeriod.startAt(),
          weeklyDisplayPeriod.endAt(),
          pageable);
    }

    NoticeDisplayPeriod displayPeriod =
        tag == NoticeTag.WEEKLY ? weeklyDisplayPeriod : dailyDisplayPeriod;
    LocalDateTime startAt = displayPeriod.startAt();
    LocalDateTime endAt = displayPeriod.endAt();
    return noticeRepository.findByGroup_IdAndTagAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        groupId, tag, startAt, endAt, pageable);
  }

  private GroupMember getGroupMember(Long groupId, Long userId) {
    return groupMemberRepository
        .findByGroupIdAndUserId(groupId, userId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
  }

  private NoticeResponse toNoticeResponse(Notice notice) {
    return NoticeResponse.of(notice, fileService.getReadableUrl(notice.getImageUrl()));
  }

  private NoticeListResponse toNoticeListResponse(Notice notice) {
    return NoticeListResponse.of(notice, fileService.getReadableUrl(notice.getImageUrl()));
  }
}
