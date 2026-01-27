package com.app.gamehub.repository;

import com.app.gamehub.entity.DynastyPosition;
import com.app.gamehub.entity.PositionType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 王朝官职数据访问接口
 */
@Repository
public interface DynastyPositionRepository extends JpaRepository<DynastyPosition, Long> {

    /**
     * 根据王朝ID查找所有官职配置
     */
    List<DynastyPosition> findByDynastyId(Long dynastyId);

    /**
     * 根据王朝ID和官职类型查找官职配置
     */
    Optional<DynastyPosition> findByDynastyIdAndPositionType(
        Long dynastyId, PositionType positionType);

    /**
     * 查找预约结束时间在指定时间之前的官职配置
     */
    List<DynastyPosition> findByReservationEndTimeBefore(LocalDateTime time);

    List<DynastyPosition> findByReservationEndTimeBeforeAndDutyDateBefore(LocalDateTime time, LocalDate date);

    /**
     * 根据王朝ID删除所有官职配置
     */
    @Modifying
    @Query("DELETE FROM DynastyPosition dp WHERE dp.dynastyId = :dynastyId")
    void deleteByDynastyId(@Param("dynastyId") Long dynastyId);
}
