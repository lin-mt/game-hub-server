package com.app.gamehub.repository;

import com.app.gamehub.entity.Alliance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AllianceRepository extends JpaRepository<Alliance, Long> {

  Optional<Alliance> findByCode(String code);

  boolean existsByCode(String code);

  List<Alliance> findByLeaderIdOrderByServerIdDesc(Long leaderId);

  List<Alliance> findByServerIdOrderByCreatedAtDesc(Integer serverId);

  @Query("SELECT COUNT(ga) FROM GameAccount ga WHERE ga.allianceId = :allianceId")
  long countMembersByAllianceId(@Param("allianceId") Long allianceId);

  @Query("SELECT a FROM Alliance a WHERE a.guanduRegistrationStartDay = :dayOfWeek AND a.guanduRegistrationStartDay IS NOT NULL AND a.guanduRegistrationStartMinute IS NOT NULL AND a.guanduRegistrationEndDay IS NOT NULL AND a.guanduRegistrationEndMinute IS NOT NULL")
  List<Alliance> findByGuanduRegistrationStartDay(@Param("dayOfWeek") Integer dayOfWeek);
}
