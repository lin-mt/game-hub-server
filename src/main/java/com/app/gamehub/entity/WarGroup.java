package com.app.gamehub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "war_groups")
public class WarGroup extends BaseEntity {

  @Column(name = "alliance_id", nullable = false)
  private Long allianceId;

  @Column(name = "war_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private WarType warType;

  @Column(name = "group_name", nullable = false)
  private String groupName;

  @Column(name = "group_task")
  private String groupTask;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "alliance_id", insertable = false, updatable = false)
  private Alliance alliance;
}
