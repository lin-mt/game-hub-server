package com.app.gamehub.dto;

import com.app.gamehub.entity.GameAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "游戏账号信息（包含申请时间和是否为替补）")
public class GameAccountWithApplicationTime {

  private Long id;

  private Long userId;

  private String accountName;

  private Long powerValue;

  private BigDecimal damageBonus;

  private Integer troopLevel;

  private Integer rallyCapacity;

  private Long troopQuantity;

  private Integer infantryDefense;

  private Integer infantryHp;

  private Integer archerAttack;

  private Integer archerSiege;

  private BigDecimal lvbuStarLevel;

  private Long allianceId;

  private Long dynastyId;

  private Long barbarianGroupId;

  private GameAccount.MemberTier memberTier;

  @Schema(description = "申请时间（仅官渡战事有效）")
  private LocalDateTime applicationTime;

  @Schema(description = "是否为替补人员")
  private Boolean isSubstitute = false;

  public static GameAccountWithApplicationTime from(GameAccount account, LocalDateTime applicationTime, Boolean isSubstitute) {
    GameAccountWithApplicationTime result = new GameAccountWithApplicationTime();
    
    // Copy all fields from GameAccount
    result.setId(account.getId());
    result.setUserId(account.getUserId());
    result.setAccountName(account.getAccountName());
    result.setPowerValue(account.getPowerValue());
    result.setDamageBonus(account.getDamageBonus());
    result.setTroopLevel(account.getTroopLevel());
    result.setRallyCapacity(account.getRallyCapacity());
    result.setTroopQuantity(account.getTroopQuantity());
    result.setInfantryDefense(account.getInfantryDefense());
    result.setInfantryHp(account.getInfantryHp());
    result.setArcherAttack(account.getArcherAttack());
    result.setArcherSiege(account.getArcherSiege());
    result.setLvbuStarLevel(account.getLvbuStarLevel());
    result.setBarbarianGroupId(account.getBarbarianGroupId());
    result.setMemberTier(account.getMemberTier());
    // Set the application time and substitute flag
    result.setApplicationTime(applicationTime);
    result.setIsSubstitute(Boolean.TRUE.equals(isSubstitute));

    return result;
  }
}
