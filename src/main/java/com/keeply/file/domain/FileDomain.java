package com.keeply.file.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileDomain {
  NOTICE("notice"),
  EXPIRY_ITEM("expiry-item"),
  PROFILE("profile");

  private final String path;
}
