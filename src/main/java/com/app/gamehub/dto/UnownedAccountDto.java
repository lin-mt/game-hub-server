package com.app.gamehub.dto;

import com.app.gamehub.entity.GameAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Data
@Schema(description = "无主账号信息")
public class UnownedAccountDto {

  @Schema(description = "账号ID")
  private Long id;

  @Schema(description = "区服ID")
  private Integer serverId;

  @Schema(description = "账号名称")
  private String accountName;

  @Schema(description = "战力值")
  private Long powerValue;

  @Schema(description = "伤害加成")
  private BigDecimal damageBonus;

  @Schema(description = "部队等级")
  private Integer troopLevel;

  @Schema(description = "集结容量")
  private Integer rallyCapacity;

  @Schema(description = "部队数量")
  private Long troopQuantity;

  @Schema(description = "步兵防御力")
  private Integer infantryDefense;

  @Schema(description = "步兵生命值")
  private Integer infantryHp;

  @Schema(description = "弓兵攻击力")
  private Integer archerAttack;

  @Schema(description = "弓兵破坏力")
  private Integer archerSiege;

  @Schema(description = "吕布星级")
  private BigDecimal lvbuStarLevel;

  @Schema(description = "联盟ID")
  private Long allianceId;

  @Schema(description = "王朝ID")
  private Long dynastyId;

  @Schema(description = "蛮族群ID")
  private Long barbarianGroupId;

  @Schema(description = "成员阶位")
  private GameAccount.MemberTier memberTier;

  @Schema(description = "是否为联盟正式成员")
  private Boolean allianceFormalMember;

  public static UnownedAccountDto fromGameAccount(GameAccount account) {
    UnownedAccountDto dto = new UnownedAccountDto();
    dto.setId(account.getId());
    dto.setServerId(account.getServerId());
    dto.setAccountName(account.getAccountName());
    dto.setPowerValue(account.getPowerValue());
    dto.setDamageBonus(account.getDamageBonus());
    dto.setTroopLevel(account.getTroopLevel());
    dto.setRallyCapacity(account.getRallyCapacity());
    dto.setTroopQuantity(account.getTroopQuantity());
    dto.setInfantryDefense(account.getInfantryDefense());
    dto.setInfantryHp(account.getInfantryHp());
    dto.setArcherAttack(account.getArcherAttack());
    dto.setArcherSiege(account.getArcherSiege());
    dto.setLvbuStarLevel(account.getLvbuStarLevel());
    dto.setAllianceId(account.getAllianceId());
    dto.setDynastyId(account.getDynastyId());
    dto.setBarbarianGroupId(account.getBarbarianGroupId());
    dto.setMemberTier(account.getMemberTier());
    dto.setAllianceFormalMember(account.getAllianceFormalMember());
    return dto;
  }
}