package com.keeply.expiry.dto.request;

import com.keeply.expiry.entity.ExpiryItemCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateExpiryItemRequest {

  @Schema(description = "상품명", example = "삼각김밥 전주비빔")
  private String productName;

  @Schema(description = "유통기한", example = "2026-07-11")
  private LocalDate expireDate;

  @Schema(description = "상품 카테고리", example = "DAIRY")
  private ExpiryItemCategory category;

  @Schema(description = "상품 이미지 URL", example = "https://example.com/expiry-items/updated.png")
  private String imageUrl;

  @AssertTrue(message = "수정할 항목이 최소 하나 이상 있어야 합니다.")
  public boolean isUpdateFieldPresent() {
    return productName != null || expireDate != null || category != null || imageUrl != null;
  }

  @AssertTrue(message = "상품명/이미지 URL은 공백일 수 없습니다.")
  public boolean isFieldNotBlank() {
    return !((productName != null && productName.isBlank())
        || (imageUrl != null && imageUrl.isBlank()));
  }
}
