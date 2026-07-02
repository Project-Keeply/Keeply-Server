package com.keeply.notice.domain;

import com.keeply.notice.entity.Notice;
import com.keeply.notice.entity.NoticeTag;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

public record NoticeDisplayPeriod(LocalDateTime startAt, LocalDateTime endAt) {

  public static NoticeDisplayPeriod from(Notice notice) {
    LocalDate createdDate = notice.getCreatedAt().toLocalDate();
    LocalDateTime startAt =
        notice.getTag() == NoticeTag.WEEKLY
            ? createdDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay()
            : createdDate.atStartOfDay();
    LocalDateTime endAt =
        notice.getTag() == NoticeTag.WEEKLY ? startAt.plusWeeks(1) : startAt.plusDays(1);
    return new NoticeDisplayPeriod(startAt, endAt);
  }
}
