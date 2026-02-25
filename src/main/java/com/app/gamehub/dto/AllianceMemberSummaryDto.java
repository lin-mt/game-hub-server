package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Data
@Schema(description = "联盟成员简要信息")
public class AllianceMemberSummaryDto {

  @Schema(description = "账号ID")
  private Long id;

  @Schema(description = "用户ID")
  private Long userId;

  @Schema(description = "阶位")
  private String memberTier;

  @Schema(description = "是否为联盟正式成员")
  private Boolean allianceFormalMember;

  @Schema(description = "账号名称")
  private String accountName;

  @Schema(description = "吕布星级")
  private BigDecimal lvbuStarLevel;

  @Schema(description = "最高加成")
  private BigDecimal damageBonus;

  @Schema(description = "战力（万）")
  private Long powerValue;

  @Schema(description = "兵等级")
  private Integer troopLevel;

  @Schema(description = "步兵防御力")
  private Integer infantryDefense;

  @Schema(description = "步兵生命值")
  private Integer infantryHp;

  @Schema(description = "弓兵攻击力")
  private Integer archerAttack;

  @Schema(description = "弓兵破坏力")
  private Integer archerSiege;
}
