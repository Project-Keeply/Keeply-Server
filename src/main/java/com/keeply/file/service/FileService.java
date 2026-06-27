package com.keeply.file.service;

import com.keeply.file.dto.request.PresignedUploadUrlRequest;
import com.keeply.file.dto.response.PresignedDownloadUrlResponse;
import com.keeply.file.dto.response.PresignedUploadUrlResponse;

public interface FileService {

  PresignedUploadUrlResponse createUploadUrl(PresignedUploadUrlRequest request);

  PresignedDownloadUrlResponse createDownloadUrl(String fileKey);

  void validateUploadedFile(String fileKey);
}
