package com.keeply.group.util;

import com.keeply.common.exception.CustomException;
import com.keeply.common.exception.ErrorCode;
import com.keeply.group.repository.GroupRepository;
import jakarta.persistence.EntityManager;
import java.security.SecureRandom;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InviteCodeGenerator {

  private static final String CHARSET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
  private static final int CODE_LENGTH = 6;
  private static final int MAX_RETRIES = 10;

  private final GroupRepository groupRepository;
  private final EntityManager entityManager;
  private final SecureRandom random = new SecureRandom();

  public <T> T generateAndPersist(Function<String, T> persister) {
    for (int i = 0; i < MAX_RETRIES; i++) {
      String inviteCode = generateCode();
      if (groupRepository.existsByInviteCode(inviteCode)) {
        continue;
      }
      try {
        T result = persister.apply(inviteCode);
        entityManager.flush();
        return result;
      } catch (DataIntegrityViolationException e) {
        if (!isInviteCodeConflict(e)) {
          throw e;
        }
      }
    }
    throw new CustomException(ErrorCode.INVITE_CODE_GENERATION_FAILED);
  }

  private boolean isInviteCodeConflict(DataIntegrityViolationException e) {
    Throwable cause = e.getMostSpecificCause();
    if (cause == null || cause.getMessage() == null) {
      return false;
    }
    return cause.getMessage().toLowerCase().contains("invite_code");
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
