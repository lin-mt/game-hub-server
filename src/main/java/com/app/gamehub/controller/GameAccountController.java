package com.app.gamehub.controller;

import com.app.gamehub.dto.ApiResponse;
import com.app.gamehub.dto.CreateGameAccountRequest;
import com.app.gamehub.dto.JoinDynastyRequest;
import com.app.gamehub.dto.UpdateGameAccountRequest;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.service.DynastyService;
import com.app.gamehub.service.GameAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/game-accounts")
@Tag(name = "游戏账号管理", description = "游戏账号相关接口")
public class GameAccountController {

  private final GameAccountService gameAccountService;
  private final DynastyService dynastyService;

  public GameAccountController(
      GameAccountService gameAccountService, DynastyService dynastyService) {
    this.gameAccountService = gameAccountService;
    this.dynastyService = dynastyService;
  }

  @PostMapping
  @Operation(summary = "创建游戏账号")
  public ApiResponse<GameAccount> createGameAccount(
      @Valid @RequestBody CreateGameAccountRequest request) {
    GameAccount account = gameAccountService.createGameAccount(request);
    return ApiResponse.success("游戏账号创建成功", account);
  }

  @PutMapping("/{accountId}")
  @Operation(summary = "更新游戏账号")
  public ApiResponse<GameAccount> updateGameAccount(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId,
      @Valid @RequestBody UpdateGameAccountRequest request) {
    GameAccount account = gameAccountService.updateGameAccount(accountId, request);
    return ApiResponse.success("游戏账号更新成功", account);
  }

  @PostMapping(value = "/{accountId}/stats", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "上传账号属性截图并更新步兵/弓兵四项属性（步兵防御力/步兵生命值/弓兵攻击力/弓兵破坏力）")
  public ApiResponse<GameAccount> uploadStatsImage(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId,
      @Parameter(description = "包含属性截图的图片文件") @RequestPart("file") MultipartFile file) {
    GameAccount updated = gameAccountService.updateStatsFromImage(accountId, file);
    return ApiResponse.success("账号属性已更新", updated);
  }

  @DeleteMapping("/{accountId}")
  @Operation(summary = "删除游戏账号")
  public ApiResponse<Void> deleteGameAccount(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId) {
    gameAccountService.deleteGameAccount(accountId);
    return ApiResponse.success("游戏账号删除成功", null);
  }

  @GetMapping("/my")
  @Operation(summary = "获取我的游戏账号列表")
  public ApiResponse<List<GameAccount>> getMyGameAccounts() {
    List<GameAccount> accounts = gameAccountService.getUserGameAccounts();
    return ApiResponse.success(accounts);
  }

  @GetMapping("/{accountId}")
  @Operation(summary = "获取游戏账号详情")
  public ApiResponse<GameAccount> getGameAccountById(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId) {
    GameAccount account = gameAccountService.getGameAccountById(accountId);
    return ApiResponse.success(account);
  }

  @PostMapping("/{accountId}/join-dynasty")
  @Operation(summary = "账号加入王朝")
  public ApiResponse<GameAccount> joinDynasty(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId,
      @Valid @RequestBody JoinDynastyRequest request) {
    GameAccount account = dynastyService.joinDynasty(accountId, request);
    return ApiResponse.success("账号加入王朝成功", account);
  }
}
