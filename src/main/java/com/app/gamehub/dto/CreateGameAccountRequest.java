package com.app.gamehub.dto;

import com.app.gamehub.validation.ValidLvbuStarLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Data;

@Data
@Schema(description = "创建游戏账号请求")
public class CreateGameAccountRequest {

  @NotNull(message = "区号不能为空")
  @Positive(message = "区号必须为正整数")
  @Schema(description = "区号")
  private Integer serverId;

  @NotBlank(message = "账号名称不能为空")
  @Schema(description = "账号名称")
  private String accountName;

  @PositiveOrZero(message = "战力值不能为负数")
  @Schema(description = "战力值")
  private Long powerValue;

  @DecimalMin(value = "0.00", message = "伤害加成不能为负数")
  @DecimalMax(value = "999.99", message = "伤害加成不能超过999.99")
  @Digits(integer = 3, fraction = 2, message = "伤害加成最多3位整数2位小数")
  @Schema(description = "伤害加成")
  private BigDecimal damageBonus = new BigDecimal("0.0");

  @Min(value = 1, message = "兵等级最小为1")
  @Max(value = 30, message = "兵等级最大为30")
  @Schema(description = "兵等级")
  private Integer troopLevel;

  @PositiveOrZero(message = "集结容量不能为负数")
  @Schema(description = "集结容量（万）")
  private Integer rallyCapacity;

  @PositiveOrZero(message = "兵量不能为负数")
  @Schema(description = "兵量（万）")
  private Long troopQuantity;

  @PositiveOrZero(message = "步兵防御力不能为负数")
  @Schema(description = "步兵防御力（整数）")
  private Integer infantryDefense;

  @PositiveOrZero(message = "步兵生命值不能为负数")
  @Schema(description = "步兵生命值（整数）")
  private Integer infantryHp;

  @PositiveOrZero(message = "弓兵攻击力不能为负数")
  @Schema(description = "弓兵攻击力（整数）")
  private Integer archerAttack;

  @PositiveOrZero(message = "弓兵破坏力不能为负数")
  @Schema(description = "弓兵破坏力（整数）")
  private Integer archerSiege;

  @ValidLvbuStarLevel
  @Schema(description = "吕布星级（0-5星，遵循0.5进1规则）", example = "0.0")
  private BigDecimal lvbuStarLevel;

  @Schema(description = "联盟ID（可选，如果提供则创建账号时直接加入联盟）")
  private Long allianceId;
}
