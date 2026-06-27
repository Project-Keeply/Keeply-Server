package com.keeply.file.controller;

import com.keeply.common.response.ApiResponse;
import com.keeply.file.dto.request.PresignedUploadUrlRequest;
import com.keeply.file.dto.response.PresignedDownloadUrlResponse;
import com.keeply.file.dto.response.PresignedUploadUrlResponse;
import com.keeply.file.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

  private final FileService fileService;

  @PostMapping("/presigned-url")
  public ApiResponse<PresignedUploadUrlResponse> createUploadUrl(
      @AuthenticationPrincipal Long userId, @RequestBody @Valid PresignedUploadUrlRequest request) {
    PresignedUploadUrlResponse response = fileService.createUploadUrl(request);
    return ApiResponse.success(response);
  }

  @GetMapping("/access-url")
  public ApiResponse<PresignedDownloadUrlResponse> createDownloadUrl(
      @AuthenticationPrincipal Long userId, @RequestParam("key") String fileKey) {
    PresignedDownloadUrlResponse response = fileService.createDownloadUrl(fileKey);
    return ApiResponse.success(response);
  }
}
