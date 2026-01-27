package com.app.gamehub.dto;

import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.entity.WarArrangement;
import com.app.gamehub.entity.WarGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
@Schema(description = "联盟详情响应")
public class AllianceDetailResponse {

  @Schema(description = "联盟基本信息")
  private Alliance alliance;

  @Schema(description = "联盟成员列表")
  private List<GameAccount> members;

  @Schema(description = "战事分组列表（按战事类型分组）")
  private Map<String, List<WarGroup>> warGroups;

  @Schema(description = "战事人员安排列表")
  private List<WarArrangement> warArrangements;
}
