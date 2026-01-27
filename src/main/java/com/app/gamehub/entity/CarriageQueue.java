package com.app.gamehub.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * 联盟马车排队实体
 * 用于管理联盟成员开马车的排队顺序
 */
@Getter
@Setter
@Entity
@Table(name = "carriage_queues",
       uniqueConstraints = @UniqueConstraint(columnNames = {"alliance_id", "account_id"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CarriageQueue extends BaseEntity {

  /**
   * 联盟ID
   */
  @Column(name = "alliance_id", nullable = false)
  private Long allianceId;

  /**
   * 游戏账号ID
   */
  @Column(name = "account_id", nullable = false)
  private Long accountId;

  /**
   * 排队顺序（从1开始，数字越小越靠前）
   */
  @Column(name = "queue_order", nullable = false)
  private Integer queueOrder;

  /**
   * 最后开车日期（用于判断今日是否已开车）
   */
  @Column(name = "last_drive_date")
  private LocalDate lastDriveDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "alliance_id", insertable = false, updatable = false)
  private Alliance alliance;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", insertable = false, updatable = false)
  private GameAccount gameAccount;
}
