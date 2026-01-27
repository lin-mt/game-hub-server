package com.app.gamehub.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;

/**
 * 百度OCR服务额度记录（每月）
 */
@Data
@Entity
@Table(
    name = "baidu_ocr_quota",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"service_type", "quota_month"})})
public class BaiduOcrQuota {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 服务类型 */
  @Column(name = "service_type", nullable = false, length = 50)
  private String serviceType;

  /** 额度月份（格式：yyyy-MM） */
  @Column(name = "quota_month", nullable = false, length = 7)
  private String quotaMonth;

  /** 总额度 */
  @Column(name = "total_quota", nullable = false)
  private Integer totalQuota;

  /** 已使用额度 */
  @Column(name = "used_quota", nullable = false)
  private Integer usedQuota = 0;

  /** 剩余额度 */
  @Column(name = "remaining_quota", nullable = false)
  private Integer remainingQuota;

  /** 最后请求时间戳（毫秒） */
  @Column(name = "last_request_time")
  private Long lastRequestTime = 0L;

  /** 创建时间 */
  @Column(name = "created_at", nullable = false, updatable = false)
  private java.time.LocalDateTime createdAt;

  /** 更新时间 */
  @Column(name = "updated_at")
  private java.time.LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = java.time.LocalDateTime.now();
    updatedAt = java.time.LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = java.time.LocalDateTime.now();
  }

  /**
   * 递增使用次数
   */
  public void incrementUsage() {
    this.usedQuota++;
    this.remainingQuota = this.totalQuota - this.usedQuota;
  }

  /**
   * 检查是否有剩余额度
   */
  public boolean hasRemainingQuota() {
    return this.remainingQuota > 0;
  }
}
