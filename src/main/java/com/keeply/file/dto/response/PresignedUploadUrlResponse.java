package com.keeply.file.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PresignedUploadUrlResponse {

  private final String presignedUrl;
  private final String fileKey;
  private final String accessUrl;

  public static PresignedUploadUrlResponse of(
      String presignedUrl, String fileKey, String accessUrl) {
    return PresignedUploadUrlResponse.builder()
        .presignedUrl(presignedUrl)
        .fileKey(fileKey)
        .accessUrl(accessUrl)
        .build();
  }
}
