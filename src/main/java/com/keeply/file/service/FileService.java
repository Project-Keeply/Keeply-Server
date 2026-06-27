package com.keeply.file.service;

import com.keeply.file.dto.request.PresignedUploadUrlRequest;
import com.keeply.file.dto.response.PresignedDownloadUrlResponse;
import com.keeply.file.dto.response.PresignedUploadUrlResponse;

public interface FileService {

  PresignedUploadUrlResponse createUploadUrl(PresignedUploadUrlRequest request);

  PresignedDownloadUrlResponse createDownloadUrl(String fileKey);

  /**
   * 업로드된 파일을 사후 검증한다 (사이즈/MIME).
   *
   * <p>이 메서드는 의도적으로 컨트롤러에 노출되지 않는다. 도메인 서비스(NoticeService, ExpiryItemService, ProfileService)가
   * imageUrl을 DB에 저장하기 직전, {@code @Transactional} 바깥에서 호출해야 한다. HeadObject/DeleteObject는 외부 IO이므로
   * 트랜잭션 안에서 호출하면 커넥션 점유 시간이 길어지고 롤백 시 S3 부작용을 복구할 수 없다.
   *
   * @param fileKey 검증 대상 S3 객체 키
   * @throws com.keeply.common.exception.CustomException FILE_NOT_FOUND / FILE_SIZE_EXCEEDED /
   *     FILE_INVALID_MIME / FILE_PRESIGN_FAILED
   */
  void validateUploadedFile(String fileKey);
}
