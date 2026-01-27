package com.app.gamehub.util;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class AllianceCodeGenerator {

  private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int CODE_LENGTH = 6;
  private static final SecureRandom random = new SecureRandom();

  public String generateCode() {
    StringBuilder code = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
    }
    return code.toString();
  }
}
