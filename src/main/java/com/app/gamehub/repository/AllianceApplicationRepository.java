package com.app.gamehub.repository;

import com.app.gamehub.entity.AllianceApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
