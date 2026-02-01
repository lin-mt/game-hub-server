package com.app.gamehub.entity;

import com.app.gamehub.enums.ActivityType;
import com.app.gamehub.validation.ValidLvbuStarLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "game_accounts")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class GameAccount extends BaseEntity {

  @Column(name = "user_id", nullable = true)
  private Long userId;

  @Column(name = "server_id", nullable = false)
  private Integer serverId;

  @Column(name = "account_name", nullable = false)
  private String accountName;

  @Column(name = "power_value")
  private Long powerValue;

  @Column(name = "damage_bonus", precision = 5, scale = 2)
  private BigDecimal damageBonus;

  @Column(name = "troop_level")
  private Integer troopLevel;

  @Column(name = "rally_capacity")
  private Integer rallyCapacity;

  @Column(name = "troop_quantity")
  private Long troopQuantity;

  // Unit stat fields added: 步兵防御力、步兵生命值、弓兵攻击力、弓兵破坏力
  @Column(name = "infantry_defense")
  private Integer infantryDefense;

  @Column(name = "infantry_hp")
  private Integer infantryHp;

  @Column(name = "archer_attack")
  private Integer archerAttack;

  @Column(name = "archer_siege")
  private Integer archerSiege;

  @ValidLvbuStarLevel
  @Column(name = "lvbu_star_level", precision = 2, scale = 1)
  private BigDecimal lvbuStarLevel;

  @Column(name = "alliance_id")
  private Long allianceId;

  @Column(name = "dynasty_id")
  private Long dynastyId;

  @Column(name = "barbarian_group_id")
  private Long barbarianGroupId;

  @Column(name = "member_tier")
  @Enumerated(EnumType.STRING)
  private MemberTier memberTier;

  @ElementCollection(targetClass = ActivityType.class)
  @CollectionTable(
      name = "game_account_notification_types",
      joinColumns = @JoinColumn(name = "account_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "activity_type")
  private Set<ActivityType> notificationTypes =
      new HashSet<>(
          Set.of(
              ActivityType.SAN_YING_ZHAN_LV_BU,
              ActivityType.GUAN_DU_BAO_MING,
              ActivityType.ZHU_JIU_LUN_YING_XIONG,
              ActivityType.NAN_MAN_RU_QIN,
              ActivityType.GONG_CHENG,
              ActivityType.SHOU_CHENG,
              ActivityType.SHUA_GONG_XUN));

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "alliance_id", insertable = false, updatable = false)
  private Alliance alliance;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dynasty_id", insertable = false, updatable = false)
  private Dynasty dynasty;

  public enum MemberTier {
    TIER_1,
    TIER_2,
    TIER_3,
    TIER_4,
    TIER_5
  }
}
