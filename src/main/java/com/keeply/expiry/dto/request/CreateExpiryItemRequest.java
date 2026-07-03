package com.keeply.expiry.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateExpiryItemRequest {

  @Schema(description = "상품명", example = "삼각김밥 참치마요")
  @NotBlank(message = "상품명은 필수입니다.")
  private String productName;

  @Schema(description = "유통기한", example = "2026-07-10")
  @NotNull(message = "유통기한은 필수입니다.")
  private LocalDate expireDate;

  @Schema(description = "상품 이미지 URL", example = "https://example.com/expiry-items/image.png")
  @NotBlank(message = "상품 이미지 URL은 필수입니다.")
  private String imageUrl;
}
