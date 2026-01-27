package com.app.gamehub.controller;

import com.app.gamehub.dto.ApiResponse;
import com.app.gamehub.dto.BarbarianGroupDetailResponse;
import com.app.gamehub.dto.BarbarianGroupResponse;
import com.app.gamehub.dto.CreateAndJoinBarbarianGroupRequest;
import com.app.gamehub.dto.CreateBarbarianGroupRequest;
import com.app.gamehub.dto.JoinBarbarianGroupRequest;
import com.app.gamehub.entity.BarbarianGroup;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.service.BarbarianGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/barbarian-groups")
@Tag(name = "南蛮入侵分组管理", description = "南蛮入侵分组相关接口")
@RequiredArgsConstructor
public class BarbarianGroupController {

  private final BarbarianGroupService barbarianGroupService;

  @PostMapping
  @Operation(summary = "创建南蛮分组")
  public ApiResponse<BarbarianGroup> createBarbarianGroup(
      @Valid @RequestBody CreateBarbarianGroupRequest request) {
    BarbarianGroup group = barbarianGroupService.createBarbarianGroup(request);
    return ApiResponse.success("南蛮分组创建成功", group);
  }

  @PostMapping("/create-and-join")
  @Operation(summary = "创建南蛮分组并加入")
  public ApiResponse<GameAccount> createAndJoinBarbarianGroup(
      @Valid @RequestBody CreateAndJoinBarbarianGroupRequest request) {
    GameAccount account = barbarianGroupService.createAndJoinBarbarianGroup(request);
    return ApiResponse.success("创建并加入南蛮分组成功", account);
  }

  @PostMapping("/join")
  @Operation(summary = "加入南蛮分组")
  public ApiResponse<GameAccount> joinBarbarianGroup(
      @Valid @RequestBody JoinBarbarianGroupRequest request) {
    GameAccount account = barbarianGroupService.joinBarbarianGroup(request);
    return ApiResponse.success("加入南蛮分组成功", account);
  }

  @DeleteMapping("/leave/{accountId}")
  @Operation(summary = "离开南蛮分组")
  public ApiResponse<GameAccount> leaveBarbarianGroup(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId) {
    GameAccount account = barbarianGroupService.leaveBarbarianGroup(accountId);
    return ApiResponse.success("离开南蛮分组成功", account);
  }

  @GetMapping("/alliance/{allianceId}")
  @Operation(summary = "获取联盟的所有南蛮分组")
  public ApiResponse<List<BarbarianGroupResponse>> getAllianceBarbarianGroups(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId) {
    List<BarbarianGroupResponse> groups = barbarianGroupService.getAllianceBarbarianGroups(allianceId);
    return ApiResponse.success(groups);
  }

  @GetMapping("/alliance/{allianceId}/queue/{queueCount}")
  @Operation(summary = "根据队列数量查询联盟中的南蛮分组")
  public ApiResponse<List<BarbarianGroupResponse>> getBarbarianGroupsByQueueCount(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId,
      @Parameter(description = "队列数量", example = "3") @PathVariable Integer queueCount) {
    List<BarbarianGroupResponse> groups = barbarianGroupService.getBarbarianGroupsByQueueCount(allianceId, queueCount);
    return ApiResponse.success(groups);
  }

  @GetMapping("/{groupId}/detail")
  @Operation(summary = "获取分组详情（包含成员信息）")
  public ApiResponse<BarbarianGroupDetailResponse> getBarbarianGroupDetail(
      @Parameter(description = "分组ID", example = "1") @PathVariable Long groupId) {
    BarbarianGroupDetailResponse detail = barbarianGroupService.getBarbarianGroupDetail(groupId);
    return ApiResponse.success(detail);
  }

  @GetMapping("/account/{accountId}")
  @Operation(summary = "获取账号所在的南蛮分组")
  public ApiResponse<BarbarianGroupResponse> getAccountBarbarianGroup(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId) {
    BarbarianGroupResponse group = barbarianGroupService.getAccountBarbarianGroup(accountId);
    return ApiResponse.success(group);
  }
}
