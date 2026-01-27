package com.app.gamehub.controller;

import com.app.gamehub.dto.*;
import com.app.gamehub.entity.WarArrangement;
import com.app.gamehub.entity.WarGroup;
import com.app.gamehub.entity.WarType;
import com.app.gamehub.service.WarGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/war-groups")
@Tag(name = "战事分组管理", description = "战事分组相关接口")
public class WarGroupController {

  private final WarGroupService warGroupService;

  public WarGroupController(WarGroupService warGroupService) {
    this.warGroupService = warGroupService;
  }

  @PostMapping
  @Operation(summary = "创建战事分组")
  public ApiResponse<WarGroup> createWarGroup(@Valid @RequestBody CreateWarGroupRequest request) {
    WarGroup warGroup = warGroupService.createWarGroup(request);
    return ApiResponse.success("战事分组创建成功", warGroup);
  }

  @PutMapping("/{groupId}")
  @Operation(summary = "更新战事分组")
  public ApiResponse<WarGroup> updateWarGroup(
      @Parameter(description = "分组ID", example = "1") @PathVariable Long groupId,
      @Valid @RequestBody UpdateWarGroupRequest request) {
    WarGroup warGroup = warGroupService.updateWarGroup(groupId, request);
    return ApiResponse.success("战事分组更新成功", warGroup);
  }

  @DeleteMapping("/{groupId}")
  @Operation(summary = "删除战事分组")
  public ApiResponse<Void> deleteWarGroup(
      @Parameter(description = "分组ID", example = "1") @PathVariable Long groupId) {
    warGroupService.deleteWarGroup(groupId);
    return ApiResponse.success("战事分组删除成功", null);
  }

  @PostMapping("/arrange")
  @Operation(summary = "安排成员到战事分组")
  public ApiResponse<WarArrangement> arrangeMember(
      @Valid @RequestBody ArrangeMemberRequest request) {
    WarArrangement arrangement = warGroupService.arrangeMember(request);
    return ApiResponse.success("成员安排成功", arrangement);
  }

  @DeleteMapping("/alliances/{allianceId}/arrangements")
  @Operation(summary = "清空战事安排")
  public ApiResponse<Void> clearWarArrangements(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId,
      @Parameter(description = "战事类型") @RequestParam WarType warType) {
    warGroupService.clearWarArrangements(allianceId, warType);
    return ApiResponse.success("战事安排清空成功", null);
  }

  @PostMapping("/clear-arrangements")
  @Operation(summary = "清空战事安排（支持可选通知）")
  public ApiResponse<Void> clearWarArrangementsWithNotification(
      @Valid @RequestBody ClearWarArrangementsRequest request) {
    warGroupService.clearWarArrangementsWithNotification(request);
    return ApiResponse.success("战事安排清空成功", null);
  }

  @GetMapping("/alliances/{allianceId}")
  @Operation(summary = "获取联盟战事分组列表")
  public ApiResponse<List<WarGroup>> getWarGroups(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId,
      @Parameter(description = "战事类型") @RequestParam WarType warType) {
    List<WarGroup> warGroups = warGroupService.getWarGroups(allianceId, warType);
    return ApiResponse.success(warGroups);
  }

  @GetMapping("/alliances/{allianceId}/arrangements")
  @Operation(summary = "获取战事人员安排（原始数据）")
  public ApiResponse<List<WarArrangement>> getWarArrangements(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId,
      @Parameter(description = "战事类型（可选）") @RequestParam(required = false) WarType warType) {
    List<WarArrangement> arrangements;
    if (warType != null) {
      arrangements = warGroupService.getWarArrangements(allianceId, warType);
    } else {
      arrangements = warGroupService.getAllWarArrangements(allianceId);
    }
    return ApiResponse.success(arrangements);
  }

  @GetMapping("/alliances/{allianceId}/arrangements/detail")
  @Operation(summary = "获取战事人员安排详情（包含分组和机动人员）")
  public ApiResponse<WarArrangementResponse> getWarArrangementDetail(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId,
      @Parameter(description = "战事类型", required = true) @RequestParam WarType warType) {
    WarArrangementResponse response = warGroupService.getWarArrangementDetail(allianceId, warType);
    return ApiResponse.success(response);
  }

  @GetMapping("/accounts/{accountId}/arrangements")
  @Operation(summary = "获取账号的战事安排详情")
  public ApiResponse<AccountWarArrangementResponse> getAccountWarArrangements(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId) {
    AccountWarArrangementResponse response = warGroupService.getAccountWarArrangements(accountId);
    return ApiResponse.success(response);
  }
}
