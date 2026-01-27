package com.app.gamehub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "war_applications")
public class WarApplication extends BaseEntity {

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "alliance_id", nullable = false)
  private Long allianceId;

  @Column(name = "war_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private WarType warType;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private ApplicationStatus status;

  @Column(name = "processed_by")
  private Long processedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", insertable = false, updatable = false)
  private GameAccount account;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "alliance_id", insertable = false, updatable = false)
  private Alliance alliance;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_by", insertable = false, updatable = false)
  private User processor;

  @Column(name = "is_substitute", nullable = false)
  private Boolean isSubstitute = false;

  public enum ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
  }
}
