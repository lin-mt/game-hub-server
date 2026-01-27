package com.app.gamehub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "alliance_applications")
public class AllianceApplication extends BaseEntity {

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "alliance_id", nullable = false)
  private Long allianceId;

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

  public enum ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
  }
}
