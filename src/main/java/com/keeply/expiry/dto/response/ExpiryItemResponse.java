package com.keeply.expiry.dto.response;

import com.keeply.expiry.entity.ExpiryItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExpiryItemResponse {

  @Schema(description = "유통기한 상품 ID", example = "1")
  private final Long expiryItemId;

  @Schema(description = "상품명", example = "삼각김밥 참치마요")
  private final String productName;

  @Schema(description = "유통기한", example = "2026-07-10")
  private final LocalDate expireDate;

  @Schema(description = "상품 이미지 URL", example = "https://example.com/expiry-items/image.png")
  private final String imageUrl;

  @Schema(description = "오늘 기준 유통기한까지 남은 일수", example = "3")
  private final long dDay;

  @Schema(description = "작성자 유저 ID", example = "1")
  private final Long authorUserId;

  @Schema(description = "작성자 이름", example = "홍길동")
  private final String authorName;

  @Schema(description = "작성 시각", example = "2026-07-02T10:30:00")
  private final LocalDateTime createdAt;

  public static ExpiryItemResponse of(ExpiryItem expiryItem) {
    return ExpiryItemResponse.builder()
        .expiryItemId(expiryItem.getId())
        .productName(expiryItem.getProductName())
        .expireDate(expiryItem.getExpireDate())
        .imageUrl(expiryItem.getImageUrl())
        .dDay(ChronoUnit.DAYS.between(LocalDate.now(), expiryItem.getExpireDate()))
        .authorUserId(expiryItem.getAuthorMember().getUser().getId())
        .authorName(expiryItem.getAuthorMember().getUser().getName())
        .createdAt(expiryItem.getCreatedAt())
        .build();
  }
}
