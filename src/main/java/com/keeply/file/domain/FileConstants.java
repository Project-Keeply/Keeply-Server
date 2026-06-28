package com.keeply.file.domain;

import java.util.Set;

public final class FileConstants {

  public static final Set<String> ALLOWED_MIME_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  private FileConstants() {}
}
