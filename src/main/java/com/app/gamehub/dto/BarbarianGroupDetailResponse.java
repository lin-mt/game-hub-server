package com.app.gamehub.dto;

import com.app.gamehub.entity.GameAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "南蛮分组详情响应")
public class BarbarianGroupDetailResponse {

  @Schema(description = "分组基本信息")
  private BarbarianGroupResponse group;

  @Schema(description = "分组成员列表")
  private List<GameAccount> members;

  @Schema(description = "成员数量")
  private Integer memberCount;
}
