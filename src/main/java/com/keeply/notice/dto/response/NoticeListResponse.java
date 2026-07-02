package com.keeply.notice.dto.response;

import com.keeply.notice.entity.Notice;
import com.keeply.notice.entity.NoticeTag;
import io.swagger.v3.oas.annotations.media.Schema;
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

  @Schema(description = "공지 ID", example = "1")
  private final Long noticeId;

  @Schema(description = "공지 제목", example = "신상품 입고 안내")
  private final String title;

  @Schema(description = "공지 태그", example = "DAILY")
  private final NoticeTag tag;

  @Schema(description = "첨부 이미지 URL", example = "https://example.com/notices/image.png")
  private final String imageUrl;

  @Schema(description = "작성자 유저 ID", example = "1")
  private final Long authorUserId;

  @Schema(description = "작성자 이름", example = "홍길동")
  private final String authorName;

  @Schema(description = "작성 시각", example = "2026-07-02T10:30:00")
  private final LocalDateTime createdAt;

  @Schema(description = "표시 시작 시각", example = "2026-07-02T00:00:00")
  private final LocalDateTime displayStartAt;

  @Schema(description = "표시 종료 시각, exclusive 기준", example = "2026-07-03T00:00:00")
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
