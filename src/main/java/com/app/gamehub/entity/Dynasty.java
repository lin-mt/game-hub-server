package com.app.gamehub.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 王朝实体类 */
@Getter
@Setter
@Entity
@Table(name = "dynasties")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Dynasty extends BaseEntity {

  /** 王朝名称 */
  @Column(name = "name", nullable = false)
  private String name;

  /** 王朝编码（全局唯一的六位英文数字组合） */
  @Column(name = "code", unique = true, nullable = false, length = 6)
  private String code;

  /** 王朝所在区号 */
  @Column(name = "server_id", nullable = false)
  private Integer serverId;

  /** 天子用户ID */
  @Column(name = "emperor_id", nullable = false)
  private Long emperorId;

  /** 是否开启官职预约 */
  @Column(name = "reservation_enabled", nullable = false)
  private Boolean reservationEnabled = false;

  /** 是否开启自动配置官职预约 */
  @Column(name = "auto_configure_reservation", nullable = false)
  private Boolean autoConfigureReservation = false;

  /** 天子用户信息 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "emperor_id", insertable = false, updatable = false)
  private User emperor;
}
