package com.keeply.notice.service;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.common.response.PageResponse;
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
    return NoticeResponse.of(savedNotice);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<NoticeListResponse> getNoticeList(
      Long groupId, NoticeTag tag, Pageable pageable) {
    Page<Notice> notices =
        tag == null
            ? noticeRepository.findByGroup_Id(groupId, pageable)
            : noticeRepository.findByGroup_IdAndTag(groupId, tag, pageable);
    return PageResponse.of(notices.map(NoticeListResponse::of));
  }

  @Override
  @Transactional(readOnly = true)
  public NoticeResponse getNotice(Long groupId, Long noticeId) {
    Notice notice = getNoticeByIdAndGroupId(noticeId, groupId);
    return NoticeResponse.of(notice);
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
    return NoticeResponse.of(notice);
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

  private GroupMember getGroupMember(Long groupId, Long userId) {
    return groupMemberRepository
        .findByGroupIdAndUserId(groupId, userId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
  }
}
