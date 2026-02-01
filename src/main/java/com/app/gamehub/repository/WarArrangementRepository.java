package com.app.gamehub.repository;

import com.app.gamehub.entity.WarArrangement;
import com.app.gamehub.entity.WarType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface WarArrangementRepository extends JpaRepository<WarArrangement, Long> {

  List<WarArrangement> findByAllianceIdAndWarTypeOrderByCreatedAtDesc(
      Long allianceId, WarType warType);

  List<WarArrangement> findByAllianceIdOrderByCreatedAtAsc(Long allianceId);

  List<WarArrangement> findByWarGroupIdOrderByCreatedAtAsc(Long warGroupId);

  List<WarArrangement> findByAccountIdOrderByCreatedAtAsc(Long accountId);

  @Modifying
  @Query(
      "DELETE FROM WarArrangement wa WHERE wa.allianceId = :allianceId AND wa.warType = :warType")
  void deleteByAllianceIdAndWarType(
      @Param("allianceId") Long allianceId, @Param("warType") WarType warType);

  @Modifying
  @Query("DELETE FROM WarArrangement wa WHERE wa.allianceId = :allianceId")
  void deleteByAllianceId(@Param("allianceId") Long allianceId);

  List<WarArrangement> findByAccountIdAndWarTypeIn(Long id, Collection<WarType> types);

  void deleteAllByAccountId(Long id);

  void deleteByAccountIdAndWarType(Long id, WarType type);

  boolean existsByAccountIdAndWarTypeIn(Long id, Collection<WarType> types);

  /** 将指定账号的所有战事安排记录转移到另一个账号 */
  @Modifying
  @Transactional
  @Query(
      "UPDATE WarArrangement wa SET wa.accountId = :newAccountId WHERE wa.accountId = :oldAccountId")
  void transferToAccount(
      @Param("oldAccountId") Long oldAccountId, @Param("newAccountId") Long newAccountId);

  long countByAllianceIdAndWarType(Long allianceId, WarType warType);

  // Count arranged main/substitute members
  long countByAllianceIdAndWarTypeAndIsSubstitute(
      Long allianceId, WarType warType, Boolean isSubstitute);

  @Modifying
  @Query(
      "DELETE FROM WarArrangement wa WHERE wa.allianceId = :allianceId AND wa.warType IN :warTypes")
  void deleteByAllianceIdAndWarTypeIn(
      @Param("allianceId") Long allianceId, @Param("warTypes") Collection<WarType> warTypes);
}
