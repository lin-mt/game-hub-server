package com.app.gamehub.repository;

import com.app.gamehub.entity.AllianceApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AllianceApplicationRepository extends JpaRepository<AllianceApplication, Long> {

  List<AllianceApplication> findByAllianceIdAndStatusOrderByCreatedAtAsc(
      Long allianceId, AllianceApplication.ApplicationStatus status);

  Optional<AllianceApplication> findByAccountIdAndStatus(
      Long accountId, AllianceApplication.ApplicationStatus status);

  List<AllianceApplication> findByAccountIdOrderByCreatedAtDesc(Long accountId);

  boolean existsByAccountIdAndStatus(Long accountId, AllianceApplication.ApplicationStatus status);

  void deleteAllByAccountId(Long id);

  void deleteAllByAllianceId(Long allianceId);

  /** 将指定账号的所有联盟申请记录转移到另一个账号 */
  @Modifying
  @Transactional
  @Query(
      "UPDATE AllianceApplication aa SET aa.accountId = :newAccountId WHERE aa.accountId = :oldAccountId")
  void transferToAccount(
      @Param("oldAccountId") Long oldAccountId, @Param("newAccountId") Long newAccountId);

  @Modifying
  @Transactional
  @Query("DELETE FROM AllianceApplication aa WHERE aa.accountId IN :ids")
  void deleteAllByAccountIdIn(@Param("ids") java.util.Collection<Long> ids);
}
