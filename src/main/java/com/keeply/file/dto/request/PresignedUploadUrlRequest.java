package com.keeply.file.dto.request;

import com.keeply.file.domain.FileDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PresignedUploadUrlRequest {

  @NotBlank(message = "파일명은 필수입니다.")
  private String fileName;

  @NotBlank(message = "Content-Type은 필수입니다.")
  private String contentType;

  @NotNull(message = "파일 도메인은 필수입니다.")
  private FileDomain domain;
}
