package com.app.gamehub.repository;

import com.app.gamehub.entity.BarbarianGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BarbarianGroupRepository extends JpaRepository<BarbarianGroup, Long> {

  List<BarbarianGroup> findByAllianceIdOrderByCreatedAtDesc(Long allianceId);

  List<BarbarianGroup> findByAllianceIdAndQueueCountOrderByCreatedAtDesc(Long allianceId, Integer queueCount);

  @Query("SELECT COUNT(ga) FROM GameAccount ga WHERE ga.barbarianGroupId = :groupId")
  long countMembersByGroupId(@Param("groupId") Long groupId);

  void deleteByAllianceId(Long allianceId);
}
