package com.keeply.file.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "aws.s3")
public class S3Properties {

  @NotBlank private String bucket;

  @NotBlank private String region;

  @Valid private Credentials credentials = new Credentials();

  @Getter
  @Setter
  public static class Credentials {
    @NotBlank private String accessKey;
    @NotBlank private String secretKey;
  }
}
