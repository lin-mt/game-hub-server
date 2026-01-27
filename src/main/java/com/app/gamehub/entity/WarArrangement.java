package com.app.gamehub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "war_arrangements")
public class WarArrangement extends BaseEntity {

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "alliance_id", nullable = false)
  private Long allianceId;

  @Column(name = "war_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private WarType warType;

  @Column(name = "war_group_id")
  private Long warGroupId;

  @Column(name = "is_substitute", nullable = false)
  private Boolean isSubstitute = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", insertable = false, updatable = false)
  private GameAccount account;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "alliance_id", insertable = false, updatable = false)
  private Alliance alliance;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "war_group_id", insertable = false, updatable = false)
  private WarGroup warGroup;
}
