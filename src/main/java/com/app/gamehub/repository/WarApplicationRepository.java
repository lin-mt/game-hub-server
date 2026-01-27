package com.app.gamehub.repository;

import com.app.gamehub.entity.WarApplication;
import com.app.gamehub.entity.WarType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarApplicationRepository extends JpaRepository<WarApplication, Long> {

  List<WarApplication> findByAllianceIdAndWarTypeAndStatusOrderByCreatedAtAsc(
      Long allianceId, WarType warType, WarApplication.ApplicationStatus status);

  List<WarApplication> findByAccountIdOrderByCreatedAtDesc(Long accountId);

  List<WarApplication> findByAccountIdInAndWarTypeAndStatus(
      Collection<Long> accountIds, WarType warType, WarApplication.ApplicationStatus status);

  boolean existsByAccountIdAndWarTypeIn(Long accountId, Collection<WarType> warTypes);

  void deleteAllByAccountId(Long id);

  void deleteAllByAllianceId(Long id);

  void deleteByAccountIdAndWarType(Long accountId, WarType warType);

  long countByAllianceIdAndWarTypeAndStatus(Long allianceId, WarType warType, WarApplication.ApplicationStatus status);

  // Count applications filtered by substitute flag and status
  long countByAllianceIdAndWarTypeAndStatusAndIsSubstitute(Long allianceId, WarType warType, WarApplication.ApplicationStatus status, Boolean isSubstitute);

  void deleteByAllianceIdAndWarTypeIn(Long allianceId, Collection<WarType> warTypes);
}
