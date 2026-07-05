package com.keeply.notice.service;

import com.keeply.common.response.PageResponse;
import com.keeply.notice.dto.request.CreateNoticeRequest;
import com.keeply.notice.dto.request.UpdateNoticeRequest;
import com.keeply.notice.dto.response.NoticeListResponse;
import com.keeply.notice.dto.response.NoticeResponse;
import com.keeply.notice.entity.NoticeTag;
import org.springframework.data.domain.Pageable;

public interface NoticeService {

  NoticeResponse createNotice(Long userId, Long groupId, CreateNoticeRequest request);

  PageResponse<NoticeListResponse> getNoticeList(Long groupId, NoticeTag tag, Pageable pageable);

  PageResponse<NoticeListResponse> getNoticeList(
      Long groupId, NoticeTag tag, boolean isActive, Pageable pageable);

  NoticeResponse getNotice(Long groupId, Long noticeId);

  NoticeResponse updateNotice(
      Long userId, Long groupId, Long noticeId, UpdateNoticeRequest request);

  void deleteNotice(Long userId, Long groupId, Long noticeId);
}
