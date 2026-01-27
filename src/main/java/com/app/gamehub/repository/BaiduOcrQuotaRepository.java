package com.app.gamehub.repository;

import com.app.gamehub.entity.BaiduOcrQuota;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BaiduOcrQuotaRepository extends JpaRepository<BaiduOcrQuota, Long> {

  /**
   * 查找指定月份和服务类型的额度记录（加悲观锁）
   */
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT q FROM BaiduOcrQuota q WHERE q.serviceType = :serviceType AND q.quotaMonth ="
          + " :quotaMonth")
  Optional<BaiduOcrQuota> findByServiceTypeAndQuotaMonthWithLock(
      @Param("serviceType") String serviceType, @Param("quotaMonth") String quotaMonth);

  /**
   * 查找指定月份的所有额度记录，按剩余额度降序排列
   */
  @Query(
      "SELECT q FROM BaiduOcrQuota q WHERE q.quotaMonth = :quotaMonth AND q.remainingQuota > 0 ORDER"
          + " BY q.remainingQuota DESC")
  List<BaiduOcrQuota> findAvailableQuotasByMonth(@Param("quotaMonth") String quotaMonth);

  /**
   * 查找指定月份的所有额度记录
   */
  List<BaiduOcrQuota> findByQuotaMonth(String quotaMonth);

  /**
   * 原子性递增已使用额度（使用数据库原子操作，避免并发问题）
   */
  @Modifying
  @Query(
      "UPDATE BaiduOcrQuota q SET q.usedQuota = q.usedQuota + 1, q.remainingQuota = q.remainingQuota - 1, q.updatedAt = CURRENT_TIMESTAMP WHERE q.id = :id AND q.remainingQuota > 0")
  int incrementUsedQuota(@Param("id") Long id);
}
