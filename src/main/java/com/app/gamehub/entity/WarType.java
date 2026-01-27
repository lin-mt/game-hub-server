package com.app.gamehub.entity;

import java.util.Collection;
import java.util.List;
import lombok.Getter;

@Getter
public enum WarType {
  GUANDU_ONE("官渡一"),
  GUANDU_TWO("官渡二"),
  SIEGE("攻城"),
  DEFENSE("守城");

  private final String description;

  WarType(String description) {
    this.description = description;
  }

  public static boolean isGuanDu(WarType warType) {
    return GUANDU_ONE.equals(warType) || GUANDU_TWO.equals(warType);
  }

  public static Collection<WarType> allGuanDu() {
    return List.of(GUANDU_ONE, GUANDU_TWO);
  }
}
