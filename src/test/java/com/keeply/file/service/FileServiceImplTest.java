package com.keeply.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.file.config.S3Properties;
import com.keeply.file.domain.FileDomain;
import com.keeply.file.dto.request.PresignedUploadUrlRequest;
import com.keeply.file.dto.response.PresignedDownloadUrlResponse;
import com.keeply.file.dto.response.PresignedUploadUrlResponse;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

  private static final String BUCKET = "keeply-images";
  private static final String ACCESS_URL_PREFIX =
      "https://keeply-images.s3.ap-northeast-2.amazonaws.com";
  private static final String FAKE_PRESIGNED_URL =
      "https://keeply-images.s3.ap-northeast-2.amazonaws.com/object?signature=fake";

  @Mock private S3Presigner s3Presigner;
  @Mock private S3Client s3Client;
  @Mock private S3Properties s3Properties;

  @InjectMocks private FileServiceImpl fileService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(fileService, "accessUrlPrefix", ACCESS_URL_PREFIX);
  }

  // ---------- createUploadUrl ----------

  @Nested
  @DisplayName("createUploadUrl")
  class CreateUploadUrl {

    @Test
    @DisplayName("허용 MIME(image/jpeg)이면 presigned URL을 반환한다")
    void returnsUrlForAllowedMime() throws Exception {
      // given
      PresignedPutObjectRequest fake = fakePutPresigned();
      given(s3Properties.getBucket()).willReturn(BUCKET);
      given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(fake);

      PresignedUploadUrlRequest request =
          uploadRequest("photo.JPG", "image/jpeg", FileDomain.NOTICE);

      // when
      PresignedUploadUrlResponse response = fileService.createUploadUrl(request);

      // then
      assertThat(response.getPresignedUrl()).isEqualTo(FAKE_PRESIGNED_URL);
      assertThat(response.getFileKey()).matches("^notice/\\d{4}/\\d{2}/[0-9a-f-]+\\.jpg$");
      assertThat(response.getAccessUrl())
          .isEqualTo(ACCESS_URL_PREFIX + "/" + response.getFileKey());
    }

    @Test
    @DisplayName("차단 MIME(image/gif)이면 FILE_INVALID_MIME 예외를 던진다")
    void throwsOnDisallowedMime() {
      PresignedUploadUrlRequest request =
          uploadRequest("photo.gif", "image/gif", FileDomain.NOTICE);

      assertThatThrownBy(() -> fileService.createUploadUrl(request))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.FILE_INVALID_MIME);
    }

    @ParameterizedTest(name = "contentType={0} → 확장자={1}")
    @CsvSource({"image/jpeg, jpg", "image/png, png", "image/webp, webp"})
    @DisplayName("fileKey 확장자는 fileName이 아닌 contentType에서 도출된다")
    void derivesExtensionFromContentType(String contentType, String expectedExtension)
        throws Exception {
      PresignedPutObjectRequest fake = fakePutPresigned();
      given(s3Properties.getBucket()).willReturn(BUCKET);
      given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(fake);

      // fileName에는 일부러 확장자를 다르게 또는 비워서 신뢰하지 않음을 검증
      PresignedUploadUrlRequest request =
          uploadRequest("untrusted.exe", contentType, FileDomain.NOTICE);

      PresignedUploadUrlResponse response = fileService.createUploadUrl(request);

      assertThat(response.getFileKey()).endsWith("." + expectedExtension);
    }

    @ParameterizedTest
    @EnumSource(FileDomain.class)
    @DisplayName("각 도메인의 path가 fileKey prefix로 매핑된다")
    void mapsDomainPrefixCorrectly(FileDomain domain) throws Exception {
      PresignedPutObjectRequest fake = fakePutPresigned();
      given(s3Properties.getBucket()).willReturn(BUCKET);
      given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(fake);

      PresignedUploadUrlRequest request = uploadRequest("x.jpg", "image/jpeg", domain);

      PresignedUploadUrlResponse response = fileService.createUploadUrl(request);

      assertThat(response.getFileKey()).startsWith(domain.getPath() + "/");
    }

    @Test
    @DisplayName("PutObjectRequest에 bucket/key/contentType이 모두 설정된다")
    void putObjectRequestContainsBucketKeyContentType() throws Exception {
      PresignedPutObjectRequest fake = fakePutPresigned();
      given(s3Properties.getBucket()).willReturn(BUCKET);
      given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(fake);

      PresignedUploadUrlRequest request =
          uploadRequest("photo.jpg", "image/jpeg", FileDomain.PROFILE);

      PresignedUploadUrlResponse response = fileService.createUploadUrl(request);

      ArgumentCaptor<PutObjectPresignRequest> captor =
          ArgumentCaptor.forClass(PutObjectPresignRequest.class);
      verify(s3Presigner).presignPutObject(captor.capture());

      assertThat(captor.getValue().putObjectRequest().bucket()).isEqualTo(BUCKET);
      assertThat(captor.getValue().putObjectRequest().key()).isEqualTo(response.getFileKey());
      assertThat(captor.getValue().putObjectRequest().contentType()).isEqualTo("image/jpeg");
    }
  }

  // ---------- createDownloadUrl ----------

  @Nested
  @DisplayName("createDownloadUrl")
  class CreateDownloadUrl {

    @Test
    @DisplayName("presigned GET URL을 반환한다")
    void returnsPresignedGetUrl() throws Exception {
      PresignedGetObjectRequest fake = fakeGetPresigned();
      given(s3Properties.getBucket()).willReturn(BUCKET);
      given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(fake);

      PresignedDownloadUrlResponse response =
          fileService.createDownloadUrl("notice/2026/06/abc.jpg");

      assertThat(response.getPresignedUrl()).isEqualTo(FAKE_PRESIGNED_URL);
    }
  }

  // ---------- validateUploadedFile ----------

  @Nested
  @DisplayName("validateUploadedFile")
  class ValidateUploadedFile {

    @Test
    @DisplayName("사이즈/MIME 모두 유효하면 예외 없이 통과")
    void passesWhenSizeAndMimeAreValid() {
      given(s3Properties.getBucket()).willReturn(BUCKET);
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willReturn(headResponse(5L * 1024 * 1024, "image/jpeg"));

      fileService.validateUploadedFile("notice/2026/06/abc.jpg");

      verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("사이즈가 10MB를 초과하면 DeleteObject 호출 후 FILE_SIZE_EXCEEDED 예외")
    void deletesAndThrowsWhenSizeExceeds() {
      given(s3Properties.getBucket()).willReturn(BUCKET);
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willReturn(headResponse(11L * 1024 * 1024, "image/jpeg"));

      assertThatThrownBy(() -> fileService.validateUploadedFile("notice/2026/06/abc.jpg"))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED);

      verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("contentType이 화이트리스트 밖이면 DeleteObject 호출 후 FILE_INVALID_MIME 예외")
    void deletesAndThrowsWhenMimeInvalid() {
      given(s3Properties.getBucket()).willReturn(BUCKET);
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willReturn(headResponse(1L * 1024 * 1024, "image/gif"));

      assertThatThrownBy(() -> fileService.validateUploadedFile("notice/2026/06/abc.gif"))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.FILE_INVALID_MIME);

      verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("NoSuchKeyException이면 FILE_NOT_FOUND 예외, DeleteObject는 호출되지 않음")
    void throwsNotFoundWhenObjectMissing() {
      given(s3Properties.getBucket()).willReturn(BUCKET);
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willThrow(NoSuchKeyException.builder().message("not found").build());

      assertThatThrownBy(() -> fileService.validateUploadedFile("notice/2026/06/missing.jpg"))
          .isInstanceOf(CustomException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.FILE_NOT_FOUND);

      verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }
  }

  // ---------- helpers ----------

  private static PresignedUploadUrlRequest uploadRequest(
      String fileName, String contentType, FileDomain domain) {
    PresignedUploadUrlRequest request = new PresignedUploadUrlRequest();
    setField(request, "fileName", fileName);
    setField(request, "contentType", contentType);
    setField(request, "domain", domain);
    return request;
  }

  private static void setField(Object target, String name, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private static PresignedPutObjectRequest fakePutPresigned() throws Exception {
    PresignedPutObjectRequest result = mock(PresignedPutObjectRequest.class);
    URL url = URI.create(FAKE_PRESIGNED_URL).toURL();
    given(result.url()).willReturn(url);
    return result;
  }

  private static PresignedGetObjectRequest fakeGetPresigned() throws Exception {
    PresignedGetObjectRequest result = mock(PresignedGetObjectRequest.class);
    URL url = URI.create(FAKE_PRESIGNED_URL).toURL();
    given(result.url()).willReturn(url);
    return result;
  }

  private static HeadObjectResponse headResponse(long contentLength, String contentType) {
    return HeadObjectResponse.builder()
        .contentLength(contentLength)
        .contentType(contentType)
        .build();
  }
}
