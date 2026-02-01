package com.app.gamehub.repository;

import com.app.gamehub.entity.GameAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameAccountRepository extends JpaRepository<GameAccount, Long> {

  List<GameAccount> findByUserIdOrderByServerIdDesc(Long userId);

  List<GameAccount> findByAllianceIdOrderByPowerValueDesc(Long allianceId);

  long countByUserIdAndServerId(Long userId, Integer serverId);

  boolean existsByUserIdAndServerIdAndAllianceIdIsNotNull(Long userId, Integer serverId);

  /** 批量清空指定王朝的所有成员的王朝关联 */
  @Modifying
  @Query("UPDATE GameAccount ga SET ga.dynastyId = null WHERE ga.dynastyId = :dynastyId")
  void clearDynastyIdByDynastyId(@Param("dynastyId") Long dynastyId);

  /** 批量清空指定联盟的所有成员的联盟关联 */
  @Modifying
  @Query(
      "UPDATE GameAccount ga SET ga.allianceId = null, ga.memberTier = null, ga.barbarianGroupId = null WHERE ga.allianceId = :allianceId")
  void clearAllianceIdByAllianceId(@Param("allianceId") Long allianceId);

  List<GameAccount> findByBarbarianGroupIdOrderByPowerValueDesc(Long barbarianGroupId);

  List<GameAccount> findByAllianceId(Long id);

  // 新增：按联盟ID和账号名称查找单个账号，用于根据成员名称更新信息
  Optional<GameAccount> findByAllianceIdAndAccountName(Long allianceId, String accountName);

  Optional<GameAccount> findByAccountNameAndServerIdAndUserId(
      String accountName, Integer serverId, Long userId);

  /** 查找联盟中的无主账号（userId为null的账号） */
  List<GameAccount> findByAllianceIdAndUserIdIsNull(Long allianceId);
}
