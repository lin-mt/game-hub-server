package com.app.gamehub.repository;

import com.app.gamehub.entity.Dynasty;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 王朝数据访问接口 */
@Repository
public interface DynastyRepository extends JpaRepository<Dynasty, Long> {

  /** 根据王朝编码查找王朝 */
  Optional<Dynasty> findByCode(String code);

  /** 检查王朝编码是否存在 */
  boolean existsByCode(String code);

  /** 根据天子ID查找王朝列表，按区号降序排序 */
  List<Dynasty> findByEmperorIdOrderByServerIdDesc(Long emperorId);

  /** 根据区号查找王朝列表，按创建时间降序排序 */
  List<Dynasty> findByServerIdOrderByCreatedAtDesc(Integer serverId);

  /** 查找开启了官职预约且开启了自动配置的王朝 */
  List<Dynasty> findByReservationEnabledTrueAndAutoConfigureReservationTrue();

  /** 检查用户在指定区是否已创建王朝 */
  boolean existsByEmperorIdAndServerId(Long emperorId, Integer serverId);

  /** 统计王朝成员数量 */
  @Query("SELECT COUNT(ga) FROM GameAccount ga WHERE ga.dynastyId = :dynastyId")
  long countMembersByDynastyId(@Param("dynastyId") Long dynastyId);
}
