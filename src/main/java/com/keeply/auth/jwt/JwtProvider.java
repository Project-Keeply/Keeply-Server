package com.keeply.auth.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProvider {
  private String secret;
  private long accessTokenExpiration;
  private long refreshTokenExpiration;
}
