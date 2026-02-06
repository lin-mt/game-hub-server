package com.app.gamehub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tactic_template")
public class TacticTemplate extends BaseEntity {

  @Column(name = "alliance_id")
  private Long allianceId;

  @Column(name = "war_type")
  @Enumerated(EnumType.STRING)
  private WarType warType;

  @Column(name = "template_type")
  private String templateType;

  @Column(name = "name")
  private String name;

  @Column(name = "tactic_key", unique = true, nullable = false)
  private String tacticKey;

  // JSON configuration (text)
  @Column(name = "config_json", columnDefinition = "TEXT")
  private String configJson;

}
