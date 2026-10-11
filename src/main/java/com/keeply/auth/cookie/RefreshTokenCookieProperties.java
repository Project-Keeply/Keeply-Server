package com.keeply.auth.cookie;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.auth-cookie")
public class RefreshTokenCookieProperties {

  private boolean isSecure = false;

  @NotBlank
  @Pattern(regexp = "Lax|Strict|None", message = "SameSite는 Lax, Strict, None 중 하나여야 합니다.")
  private String sameSite = "Lax";

  @AssertTrue(message = "SameSite=None 쿠키는 Secure=true 설정이 필요합니다.")
  public boolean isSecureForSameSite() {
    return !"None".equals(sameSite) || isSecure;
  }
}
