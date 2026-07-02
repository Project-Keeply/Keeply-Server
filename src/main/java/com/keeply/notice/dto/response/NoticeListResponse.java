package com.keeply.notice.dto.response;

import com.keeply.notice.entity.Notice;
import com.keeply.notice.entity.NoticeTag;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NoticeListResponse {

  private final Long noticeId;
  private final String title;
  private final NoticeTag tag;
  private final String imageUrl;
  private final Long authorUserId;
  private final String authorName;
  private final LocalDateTime createdAt;
  private final LocalDateTime displayStartAt;
  private final LocalDateTime displayEndAt;

  public static NoticeListResponse of(Notice notice) {
    return NoticeListResponse.builder()
        .noticeId(notice.getId())
        .title(notice.getTitle())
        .tag(notice.getTag())
        .imageUrl(notice.getImageUrl())
        .authorUserId(notice.getAuthorMember().getUser().getId())
        .authorName(notice.getAuthorMember().getUser().getName())
        .createdAt(notice.getCreatedAt())
        .displayStartAt(calculateDisplayStartAt(notice))
        .displayEndAt(calculateDisplayEndAt(notice))
        .build();
  }

  private static LocalDateTime calculateDisplayStartAt(Notice notice) {
    LocalDate createdDate = notice.getCreatedAt().toLocalDate();
    if (notice.getTag() == NoticeTag.WEEKLY) {
      return createdDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
    }
    return createdDate.atStartOfDay();
  }

  private static LocalDateTime calculateDisplayEndAt(Notice notice) {
    if (notice.getTag() == NoticeTag.WEEKLY) {
      return calculateDisplayStartAt(notice).plusWeeks(1);
    }
    return calculateDisplayStartAt(notice).plusDays(1);
  }
}
