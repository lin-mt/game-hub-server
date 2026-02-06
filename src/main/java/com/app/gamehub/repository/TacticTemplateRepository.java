package com.app.gamehub.repository;

import java.util.Optional;
import java.util.List;
import com.app.gamehub.entity.TacticTemplate;
import com.app.gamehub.entity.WarType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TacticTemplateRepository extends JpaRepository<TacticTemplate, Long> {
  Optional<TacticTemplate> findByTacticKey(String tacticKey);

  List<TacticTemplate> findByAllianceIdAndWarTypeAndTemplateTypeOrderByUpdatedAtDesc(
      Long allianceId, WarType warType, String templateType);
}

