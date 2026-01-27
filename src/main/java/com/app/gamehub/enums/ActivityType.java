package com.app.gamehub.enums;

import lombok.Getter;

@Getter
public enum ActivityType {
  SAN_YING_ZHAN_LV_BU("三英战吕布"),
  GUAN_DU_BAO_MING("官渡报名") {
    @Override
    public String page(String params) {
      return "pages/game/sanguo/account/detail?" + params;
    }
  },
  ZHU_JIU_LUN_YING_XIONG("煮酒论英雄"),
  NAN_MAN_RU_QIN("南蛮入侵") {
    @Override
    public String page(String params) {
      return "pages/game/sanguo/account/detail?" + params;
    }
  },
  GONG_CHENG("攻城") {
    @Override
    public String page(String params) {
      return "pages/game/sanguo/account/detail?" + params;
    }
  },
  SHOU_CHENG("守城") {
    @Override
    public String page(String params) {
      return "pages/game/sanguo/account/detail?" + params;
    }
  },
  SHUA_GONG_XUN("刷功勋"),
  YAN_WU_CHANG("演武场");

  private final String displayName;

  ActivityType(String displayName) {
    this.displayName = displayName;
  }

  public String page(String params) {
    return null;
  }
}
