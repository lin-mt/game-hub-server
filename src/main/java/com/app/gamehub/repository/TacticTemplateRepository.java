package com.app.gamehub.repository;

import com.app.gamehub.entity.TacticTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TacticTemplateRepository extends JpaRepository<TacticTemplate, Long> {
  Optional<TacticTemplate> findByTacticKey(String tacticKey);
}

