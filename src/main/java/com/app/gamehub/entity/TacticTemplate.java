package com.app.gamehub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tactic_template")
public class TacticTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tactic_key", unique = true, nullable = false)
  private String tacticKey;

  // JSON configuration (text)
  @Column(name = "config_json", columnDefinition = "TEXT")
  private String configJson;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTacticKey() {
    return tacticKey;
  }

  public void setTacticKey(String tacticKey) {
    this.tacticKey = tacticKey;
  }

  public String getConfigJson() {
    return configJson;
  }

  public void setConfigJson(String configJson) {
    this.configJson = configJson;
  }
}

