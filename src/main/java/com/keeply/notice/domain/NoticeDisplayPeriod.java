package com.keeply.notice.domain;

import com.keeply.notice.entity.Notice;
import com.keeply.notice.entity.NoticeTag;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

public record NoticeDisplayPeriod(LocalDateTime startAt, LocalDateTime endAt) {

  public static NoticeDisplayPeriod from(Notice notice) {
    LocalDate createdDate = notice.getCreatedAt().toLocalDate();
    return from(notice.getTag(), createdDate);
  }

  public static NoticeDisplayPeriod from(NoticeTag tag, LocalDate baseDate) {
    Objects.requireNonNull(tag, "tag must not be null");
    Objects.requireNonNull(baseDate, "baseDate must not be null");

    LocalDateTime startAt =
        tag == NoticeTag.WEEKLY
            ? baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay()
            : baseDate.atStartOfDay();
    LocalDateTime endAt = tag == NoticeTag.WEEKLY ? startAt.plusWeeks(1) : startAt.plusDays(1);
    return new NoticeDisplayPeriod(startAt, endAt);
  }
}
