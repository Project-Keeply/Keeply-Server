package com.keeply.onboarding.util;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.group.repository.GroupRepository;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InviteCodeGenerator {

  private static final String CHARSET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
  private static final int CODE_LENGTH = 6;
  private static final int MAX_RETRIES = 10;

  private final GroupRepository groupRepository;
  private final SecureRandom random = new SecureRandom();

  public String generateUniqueInviteCode() {
    for (int i = 0; i < MAX_RETRIES; i++) {
      String inviteCode = generateCode();
      if (!groupRepository.existsByInviteCode(inviteCode)) {
        return inviteCode;
      }
    }
    throw new CustomException(ErrorCode.INVITE_CODE_GENERATION_FAILED);
  }

  private String generateCode() {
    StringBuilder sb = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      int index = random.nextInt(CHARSET.length());
      sb.append(CHARSET.charAt(index));
    }
    return sb.toString();
  }
}
