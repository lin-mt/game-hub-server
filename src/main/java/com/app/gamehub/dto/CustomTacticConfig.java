package com.app.gamehub.dto;

import java.util.List;
import lombok.Data;

@Data
public class CustomTacticConfig {
  private List<Group> groups;

  @Data
  public static class Group {
    private String id;
    private String initialBuilding;
    private String initialMembers;
    private String leaderRank;
    private String wuchaoMembers;
    private List<AfterOpenBuilding> afterOpenBuildings;
  }

  @Data
  public static class AfterOpenBuilding {
    private String id;
    private String building;
    private String members;
  }
}
