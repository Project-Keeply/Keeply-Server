package com.keeply.file.service;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.file.config.S3Properties;
import com.keeply.file.domain.FileConstants;
import com.keeply.file.domain.FileDomain;
import com.keeply.file.dto.request.PresignedUploadUrlRequest;
import com.keeply.file.dto.response.PresignedDownloadUrlResponse;
import com.keeply.file.dto.response.PresignedUploadUrlResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

  private static final Duration UPLOAD_URL_DURATION = Duration.ofMinutes(5);
  private static final Duration DOWNLOAD_URL_DURATION = Duration.ofMinutes(10);
  private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

  private final S3Presigner s3Presigner;
  private final S3Client s3Client;
  private final S3Properties s3Properties;

  @Value("${app.s3.access-url-prefix}")
  private String accessUrlPrefix;

  @Override
  public PresignedUploadUrlResponse createUploadUrl(PresignedUploadUrlRequest request) {
    String contentType = request.getContentType();
    if (!FileConstants.ALLOWED_MIME_TYPES.contains(contentType)) {
      throw new CustomException(ErrorCode.FILE_INVALID_MIME);
    }

    String fileKey = generateFileKey(request.getDomain(), contentType);

    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(s3Properties.getBucket())
            .key(fileKey)
            .contentType(contentType)
            .build();

    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(UPLOAD_URL_DURATION)
            .putObjectRequest(putObjectRequest)
            .build();

    String presignedUrl;
    try {
      presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
    } catch (Exception e) {
      throw new CustomException(ErrorCode.FILE_PRESIGN_FAILED);
    }

    String accessUrl = accessUrlPrefix + "/" + fileKey;
    return PresignedUploadUrlResponse.of(presignedUrl, fileKey, accessUrl);
  }

  @Override
  public PresignedDownloadUrlResponse createDownloadUrl(String fileKey) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(s3Properties.getBucket()).key(fileKey).build();

    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(DOWNLOAD_URL_DURATION)
            .getObjectRequest(getObjectRequest)
            .build();

    String presignedUrl;
    try {
      presignedUrl = s3Presigner.presignGetObject(presignRequest).url().toString();
    } catch (Exception e) {
      throw new CustomException(ErrorCode.FILE_PRESIGN_FAILED);
    }

    return PresignedDownloadUrlResponse.of(presignedUrl);
  }

  @Override
  public void validateUploadedFile(String fileKey) {
    HeadObjectResponse headResponse = headObject(fileKey);

    if (headResponse.contentLength() != null
        && headResponse.contentLength() > MAX_FILE_SIZE_BYTES) {
      deleteObject(fileKey);
      throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
    }

    if (!FileConstants.ALLOWED_MIME_TYPES.contains(headResponse.contentType())) {
      deleteObject(fileKey);
      throw new CustomException(ErrorCode.FILE_INVALID_MIME);
    }
  }

  private HeadObjectResponse headObject(String fileKey) {
    HeadObjectRequest headRequest =
        HeadObjectRequest.builder().bucket(s3Properties.getBucket()).key(fileKey).build();
    try {
      return s3Client.headObject(headRequest);
    } catch (NoSuchKeyException e) {
      throw new CustomException(ErrorCode.FILE_NOT_FOUND);
    } catch (Exception e) {
      throw new CustomException(ErrorCode.FILE_PRESIGN_FAILED);
    }
  }

  private void deleteObject(String fileKey) {
    s3Client.deleteObject(
        DeleteObjectRequest.builder().bucket(s3Properties.getBucket()).key(fileKey).build());
  }

  private String generateFileKey(FileDomain domain, String contentType) {
    LocalDate today = LocalDate.now();
    String extension = mapContentTypeToExtension(contentType);
    String uuid = UUID.randomUUID().toString();
    return String.format(
        "%s/%d/%02d/%s.%s",
        domain.getPath(), today.getYear(), today.getMonthValue(), uuid, extension);
  }

  private String mapContentTypeToExtension(String contentType) {
    return switch (contentType) {
      case "image/jpeg" -> "jpg";
      case "image/png" -> "png";
      case "image/webp" -> "webp";
      default -> throw new CustomException(ErrorCode.FILE_INVALID_MIME);
    };
  }
}
