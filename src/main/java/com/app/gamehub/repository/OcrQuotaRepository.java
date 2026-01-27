package com.app.gamehub.repository;

import com.app.gamehub.entity.OcrQuota;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OcrQuotaRepository extends JpaRepository<OcrQuota, Long> {

  /**
   * 查找指定服务商、服务类型和月份的额度记录（加悲观锁）
   */
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT q FROM OcrQuota q WHERE q.provider = :provider AND q.serviceType = :serviceType AND q.quotaMonth = :quotaMonth")
  Optional<OcrQuota> findByProviderAndServiceTypeAndQuotaMonthWithLock(
      @Param("provider") String provider, 
      @Param("serviceType") String serviceType, 
      @Param("quotaMonth") String quotaMonth);

  /**
   * 查找指定服务商和月份的所有可用额度记录，按剩余额度降序排列
   */
  @Query(
      "SELECT q FROM OcrQuota q WHERE q.provider = :provider AND q.quotaMonth = :quotaMonth AND q.remainingQuota > 0 ORDER BY q.remainingQuota DESC")
  List<OcrQuota> findAvailableQuotasByProviderAndMonth(
      @Param("provider") String provider, 
      @Param("quotaMonth") String quotaMonth);

  /**
   * 查找指定服务商和月份的所有额度记录
   */
  List<OcrQuota> findByProviderAndQuotaMonth(String provider, String quotaMonth);

  /**
   * 查找指定月份的所有额度记录，按服务商和剩余额度排序
   */
  @Query(
      "SELECT q FROM OcrQuota q WHERE q.quotaMonth = :quotaMonth ORDER BY q.provider, q.remainingQuota DESC")
  List<OcrQuota> findAllByQuotaMonth(@Param("quotaMonth") String quotaMonth);

  /**
   * 原子性递增已使用额度（使用数据库原子操作，避免并发问题）
   */
  @Modifying
  @Query(
      "UPDATE OcrQuota q SET q.usedQuota = q.usedQuota + 1, q.remainingQuota = q.remainingQuota - 1, q.updatedAt = CURRENT_TIMESTAMP WHERE q.id = :id AND q.remainingQuota > 0")
  int incrementUsedQuota(@Param("id") Long id);
}