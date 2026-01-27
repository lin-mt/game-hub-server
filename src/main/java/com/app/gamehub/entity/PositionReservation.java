package com.app.gamehub.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** 官职预约实体类 */
@Getter
@Setter
@Entity
@Table(
    name = "position_reservations",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"dynasty_id", "position_type", "duty_date", "time_slot"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PositionReservation extends BaseEntity {

  /** 王朝ID */
  @Column(name = "dynasty_id", nullable = false)
  private Long dynastyId;

  /** 官职类型 */
  @Column(name = "position_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private PositionType positionType;

  /** 任职日期 */
  @Column(name = "duty_date", nullable = false)
  private LocalDate dutyDate;

  /** 时段（0-23，表示00:00-00:59到23:00-23:59） */
  @Column(name = "time_slot", nullable = false)
  private Integer timeSlot;

  /** 预约成功的账号ID */
  @Column(name = "account_id", nullable = false)
  private Long accountId;

  /** 王朝信息 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dynasty_id", insertable = false, updatable = false)
  private Dynasty dynasty;

  /** 预约成功的账号信息 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", insertable = false, updatable = false)
  private GameAccount account;
}
