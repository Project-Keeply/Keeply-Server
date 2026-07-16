package com.keeply.common.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

  @NotEmpty private List<String> allowedOrigins = List.of("http://localhost:5173");
}
