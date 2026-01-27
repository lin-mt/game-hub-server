package com.app.gamehub.dto;

import java.util.List;
import java.util.Map;

// Simple DTO matching stored JSON structure
public class TacticTemplateConfig {
  private List<GroupConfig> groups;

  public List<GroupConfig> getGroups() {
    return groups;
  }

  public void setGroups(List<GroupConfig> groups) {
    this.groups = groups;
  }

  public static class GroupConfig {
    private String name;
    private List<String> ranks; // allow special numeric strings like "1" or "1-3" or "10,12"
    private String task;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getRanks() {
      return ranks;
    }

    public void setRanks(List<String> ranks) {
      this.ranks = ranks;
    }

    public String getTask() {
      return task;
    }

    public void setTask(String task) {
      this.task = task;
    }
  }
}

