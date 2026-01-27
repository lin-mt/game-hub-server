package com.app.gamehub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "barbarian_groups")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BarbarianGroup extends BaseEntity {

  @Column(name = "alliance_id", nullable = false)
  private Long allianceId;

  @Column(name = "group_name", nullable = false)
  private String groupName;

  @Column(name = "queue_count", nullable = false)
  private Integer queueCount;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "alliance_id", insertable = false, updatable = false)
  private Alliance alliance;
}
