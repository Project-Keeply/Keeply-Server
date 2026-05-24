package com.keeply.auth.kakao;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "kakao")
public class KakaoProperties {

  @NotBlank private String clientId;

  @NotBlank private String clientSecret;

  @NotBlank private String redirectUri;
}
