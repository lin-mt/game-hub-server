package com.app.gamehub.dto;

import com.app.gamehub.entity.WarType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class CustomTacticResponse {
  private Long id;
  private Long allianceId;
  private WarType warType;
  private String name;
  private List<CustomTacticConfig.Group> groups;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
