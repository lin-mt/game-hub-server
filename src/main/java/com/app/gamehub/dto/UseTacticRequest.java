package com.app.gamehub.dto;

import com.app.gamehub.entity.WarType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UseTacticRequest {

  @NotNull(message = "战事类型不能为空")
  @Schema(description = "战术使用在哪一种战事类型上", example = "GUANDU_ONE")
  private WarType warType;

  @NotNull(message = "战术类型不能为空")
  @Schema(description = "战术类型（优先从数据库查找模板，若无则按枚举名查找）", example = "TACTIC_ONE")
  private String tactic;

  @Schema(description = "成员排序方式，默认为 HIGHEST_BONUS。可选 FOUR_STATS 表示按步兵生命值+步兵防御力+弓兵攻击力+弓兵破坏力 的和从高到低排序", example = "HIGHEST_BONUS")
  private TacticSortMode sortMode;
}
