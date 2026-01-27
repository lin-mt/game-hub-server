package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public enum TacticSortMode {
  @Schema(description = "按伤害加成从高到低排序（默认）", example = "HIGHEST_BONUS")
  HIGHEST_BONUS,

  @Schema(description = "按四维能力值之和从高到低排序：步兵生命值 + 步兵防御力 + 弓兵攻击力 + 弓兵破坏力", example = "FOUR_STATS")
  FOUR_STATS
}

