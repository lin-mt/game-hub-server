package com.app.gamehub.repository;

import com.app.gamehub.entity.PositionReservation;
import com.app.gamehub.entity.PositionType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** 官职预约数据访问接口 */
@Repository
public interface PositionReservationRepository extends JpaRepository<PositionReservation, Long> {

  /** 根据王朝ID、官职类型和任职日期查找预约结果 */
  List<PositionReservation> findByDynastyIdAndPositionTypeAndDutyDateOrderByTimeSlotAsc(
      Long dynastyId, PositionType positionType, LocalDate dutyDate);

  /** 根据王朝ID和任职日期查找所有预约结果 */
  List<PositionReservation> findByDynastyIdAndDutyDateOrderByPositionTypeAscTimeSlotAsc(
      Long dynastyId, LocalDate dutyDate);

  /** 检查指定时段是否已被预约 */
  boolean existsByDynastyIdAndPositionTypeAndDutyDateAndTimeSlot(
      Long dynastyId, PositionType positionType, LocalDate dutyDate, Integer timeSlot);

  /** 查找指定时段的预约记录 */
  Optional<PositionReservation> findByDynastyIdAndPositionTypeAndDutyDateAndTimeSlot(
      Long dynastyId, PositionType positionType, LocalDate dutyDate, Integer timeSlot);

  /** 根据账号ID查找预约记录 */
  List<PositionReservation> findByAccountIdOrderByDutyDateDescCreatedAtDesc(Long accountId);

  /** 检查用户在指定王朝、官职类型和任职日期下是否已经预约了时段 */
  boolean existsByAccountIdAndDynastyIdAndPositionTypeAndDutyDate(
      Long accountId, Long dynastyId, PositionType positionType, LocalDate dutyDate);

  /** 清空指定王朝和官职类型的预约结果 */
  @Modifying
  @Query(
      "DELETE FROM PositionReservation pr WHERE pr.dynastyId = :dynastyId AND pr.positionType = :positionType")
  void deleteByDynastyIdAndPositionType(
      @Param("dynastyId") Long dynastyId, @Param("positionType") PositionType positionType);

  /** 清空指定王朝的所有预约结果 */
  @Modifying
  @Query("DELETE FROM PositionReservation pr WHERE pr.dynastyId = :dynastyId")
  void deleteByDynastyId(@Param("dynastyId") Long dynastyId);

  @Modifying
  @Query("DELETE FROM PositionReservation pr WHERE pr.accountId = :accountId")
  void deleteByAccountId(@Param("accountId") Long accountId);

  @Modifying
  @Transactional
  @Query("DELETE FROM PositionReservation pr WHERE pr.accountId IN :ids")
  void deleteByAccountIdIn(@Param("ids") java.util.Collection<Long> ids);

  /** 将指定账号的所有官职预约记录转移到另一个账号 */
  @Modifying
  @Transactional
  @Query("UPDATE PositionReservation pr SET pr.accountId = :newAccountId WHERE pr.accountId = :oldAccountId")
  void transferToAccount(@Param("oldAccountId") Long oldAccountId, @Param("newAccountId") Long newAccountId);

  @Modifying
  @Query("DELETE FROM PositionReservation pr WHERE pr.dutyDate < :before")
  void deleteByDutyDateBefore(@Param("before") LocalDate before);

  PositionReservation findByAccountIdAndDynastyIdAndPositionTypeAndDutyDate(
      Long accountId,
      Long dynastyId,
      PositionType positionType,
      LocalDate dutyDate);

    List<PositionReservation> findByDutyDateBefore(LocalDate before);
}
