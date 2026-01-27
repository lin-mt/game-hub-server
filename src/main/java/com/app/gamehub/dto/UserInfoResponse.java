package com.app.gamehub.dto;

import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.Dynasty;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "用户信息响应")
public class UserInfoResponse {

  @Schema(description = "用户基本信息")
  private User user;

  @Schema(description = "用户创建的联盟列表")
  private List<Alliance> alliances;

  @Schema(description = "用户创建的王朝列表")
  private List<Dynasty> dynasties;

  @Schema(description = "用户的游戏账号列表")
  private List<GameAccount> gameAccounts;
}
