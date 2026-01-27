package com.app.gamehub.repository;

import com.app.gamehub.entity.WarGroup;
import com.app.gamehub.entity.WarType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarGroupRepository extends JpaRepository<WarGroup, Long> {

  List<WarGroup> findByAllianceIdAndWarTypeOrderByCreatedAtAsc(Long allianceId, WarType warType);

  List<WarGroup> findByAllianceIdOrderByWarTypeAscCreatedAtAsc(Long allianceId);

  void deleteByAllianceIdAndWarType(Long allianceId, WarType warType);

  void deleteByAllianceId(Long allianceId);

  Collection<WarGroup> findByAllianceIdAndWarType(Long allianceId, WarType warType);

  void deleteByAllianceIdAndWarTypeIn(Long allianceId, java.util.Collection<WarType> warTypes);
}
