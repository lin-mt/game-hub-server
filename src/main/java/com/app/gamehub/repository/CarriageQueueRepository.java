package com.app.gamehub.repository;

import com.app.gamehub.entity.CarriageQueue;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CarriageQueueRepository extends JpaRepository<CarriageQueue, Long> {

  /** 根据联盟ID查询排队列表，按排队顺序升序排列 */
  List<CarriageQueue> findByAllianceIdOrderByQueueOrderAsc(Long allianceId);

  /** 检查账号是否已在指定联盟的排队列表中 */
  boolean existsByAllianceIdAndAccountId(Long allianceId, Long accountId);

  /** 根据联盟ID和账号ID查询排队记录 */
  Optional<CarriageQueue> findByAllianceIdAndAccountId(Long allianceId, Long accountId);

  /** 获取指定联盟当前最大的排队顺序号 */
  @Query(
      "SELECT COALESCE(MAX(cq.queueOrder), 0) FROM CarriageQueue cq WHERE cq.allianceId = :allianceId")
  Integer findMaxQueueOrderByAllianceId(@Param("allianceId") Long allianceId);

  /** 根据联盟ID删除所有排队记录 */
  void deleteByAllianceId(Long allianceId);

  /** 将指定联盟中排队顺序大于指定值的记录的顺序号减1 */
  @Modifying
  @Query(
      "UPDATE CarriageQueue cq SET cq.queueOrder = cq.queueOrder - 1 WHERE cq.allianceId = :allianceId AND cq.queueOrder > :queueOrder")
  void decrementQueueOrderAfter(
      @Param("allianceId") Long allianceId, @Param("queueOrder") Integer queueOrder);

  /** 将指定联盟中排队顺序大于等于指定值的记录的顺序号加1 */
  @Modifying
  @Query(
      "UPDATE CarriageQueue cq SET cq.queueOrder = cq.queueOrder + 1 WHERE cq.allianceId = :allianceId AND cq.queueOrder >= :queueOrder")
  void incrementQueueOrderFrom(
      @Param("allianceId") Long allianceId, @Param("queueOrder") Integer queueOrder);

  /** 统计指定联盟的排队人数 */
  long countByAllianceId(Long allianceId);

  /** 查询指定联盟今日开车的成员（lastDriveDate为今日的） */
  Optional<CarriageQueue> findFirstByAllianceIdAndLastDriveDate(Long allianceId, LocalDate date);

  /** 查询指定联盟前一天的车主（lastDriveDate不为null且不是今天的） */
  List<CarriageQueue> findByAllianceIdAndLastDriveDateNotNullAndLastDriveDateNot(
      Long allianceId, LocalDate date);

  /** 查询指定联盟排队顺序最小的记录 */
  Optional<CarriageQueue> findFirstByAllianceIdOrderByQueueOrderAsc(Long allianceId);

  /** 查询所有不同的联盟ID */
  @Query("SELECT DISTINCT cq.allianceId FROM CarriageQueue cq")
  List<Long> findDistinctAllianceIds();

  void deleteByAccountId(Long accountId);

  /** 将指定账号的所有马车排队记录转移到另一个账号 */
  @Modifying
  @Transactional
  @Query(
      "UPDATE CarriageQueue cq SET cq.accountId = :newAccountId WHERE cq.accountId = :oldAccountId")
  void transferToAccount(
      @Param("oldAccountId") Long oldAccountId, @Param("newAccountId") Long newAccountId);
}
