package com.keeply.worklog.dto.response;

import com.keeply.worklog.entity.WorkLog;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkLogResponse {

  @Schema(description = "운영 로그 ID", example = "1")
  private final Long workLogId;

  @Schema(description = "운영 로그 내용", example = "냉장고 온도가 일시적으로 높아져 점검 요청했습니다.")
  private final String content;

  @Schema(description = "작성자 유저 ID", example = "1")
  private final Long authorUserId;

  @Schema(description = "작성자 이름", example = "홍길동")
  private final String authorName;

  @Schema(description = "작성 시각", example = "2026-07-02T10:30:00")
  private final LocalDateTime createdAt;

  public static WorkLogResponse of(WorkLog workLog) {
    return WorkLogResponse.builder()
        .workLogId(workLog.getId())
        .content(workLog.getContent())
        .authorUserId(workLog.getAuthorMember().getUser().getId())
        .authorName(workLog.getAuthorMember().getUser().getName())
        .createdAt(workLog.getCreatedAt())
        .build();
  }
}
