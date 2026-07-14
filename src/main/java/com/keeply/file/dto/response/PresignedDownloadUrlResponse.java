package com.keeply.file.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PresignedDownloadUrlResponse {

  private final String presignedUrl;

  public static PresignedDownloadUrlResponse of(String presignedUrl) {
    return PresignedDownloadUrlResponse.builder().presignedUrl(presignedUrl).build();
  }
}
