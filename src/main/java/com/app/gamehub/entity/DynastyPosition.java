package com.app.gamehub.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 王朝官职实体类 */
@Getter
@Setter
@Entity
@Table(name = "dynasty_positions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DynastyPosition extends BaseEntity {

  /** 王朝ID */
  @Column(name = "dynasty_id", nullable = false)
  private Long dynastyId;

  /** 官职类型 */
  @Column(name = "position_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private PositionType positionType;

  /** 预约开始时间 */
  @Column(name = "reservation_start_time")
  private LocalDateTime reservationStartTime;

  /** 预约结束时间 */
  @Column(name = "reservation_end_time")
  private LocalDateTime reservationEndTime;

  /** 任职日期 */
  @Column(name = "duty_date")
  private LocalDate dutyDate;

  /** 禁用的时段（用逗号分隔的时段列表，如"0,1,2"表示00:00-00:59、01:00-01:59、02:00-02:59被禁用） */
  @Column(name = "disabled_time_slots", length = 100)
  private String disabledTimeSlots;

  /** 王朝信息 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dynasty_id", insertable = false, updatable = false)
  private Dynasty dynasty;
}
