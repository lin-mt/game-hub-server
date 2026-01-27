package com.app.gamehub.controller;

import com.app.gamehub.dto.*;
import com.app.gamehub.entity.Alliance;
import com.app.gamehub.service.AllianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alliances")
@Tag(name = "联盟管理", description = "联盟相关接口")
public class AllianceController {

  private final AllianceService allianceService;

  public AllianceController(AllianceService allianceService) {
    this.allianceService = allianceService;
  }

  @PostMapping
  @Operation(summary = "创建联盟")
  public ApiResponse<Alliance> createAlliance(@Valid @RequestBody CreateAllianceRequest request) {
    Alliance alliance = allianceService.createAlliance(request);
    return ApiResponse.success("联盟创建成功", alliance);
  }

  @PutMapping("/{allianceId}")
  @Operation(summary = "更新联盟")
  public ApiResponse<Alliance> updateAlliance(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId,
      @Valid @RequestBody UpdateAllianceRequest request) {
    Alliance alliance = allianceService.updateAlliance(allianceId, request);
    return ApiResponse.success("联盟更新成功", alliance);
  }

  @DeleteMapping("/{allianceId}")
  @Operation(summary = "删除联盟")
  public ApiResponse<Void> deleteAlliance(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId) {
    allianceService.deleteAlliance(allianceId);
    return ApiResponse.success("联盟删除成功", null);
  }

  @PostMapping("/{allianceId}/transfer")
  @Operation(summary = "转交联盟")
  public ApiResponse<Alliance> transferAlliance(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId,
      @Valid @RequestBody TransferAllianceRequest request) {
    Alliance alliance = allianceService.transferAlliance(allianceId, request);
    return ApiResponse.success("联盟转交成功", alliance);
  }

  @GetMapping("/my")
  @Operation(summary = "获取我创建的联盟列表")
  public ApiResponse<List<Alliance>> getMyAlliances() {
    List<Alliance> alliances = allianceService.getUserAlliances();
    return ApiResponse.success(alliances);
  }

  @GetMapping("/{allianceId}")
  @Operation(summary = "获取联盟详情")
  public ApiResponse<Alliance> getAllianceById(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId) {
    Alliance alliance = allianceService.getAllianceById(allianceId);
    return ApiResponse.success(alliance);
  }

  @GetMapping("/code/{code}")
  @Operation(summary = "通过编码获取联盟信息")
  public ApiResponse<Alliance> getAllianceByCode(
      @Parameter(description = "联盟编码", example = "ABC123") @PathVariable String code) {
    Alliance alliance = allianceService.getAllianceByCode(code);
    return ApiResponse.success(alliance);
  }

  @PutMapping("/approval-settings")
  @Operation(summary = "更新联盟审核设置")
  public ApiResponse<Alliance> updateAllianceApprovalSettings(
      @Valid @RequestBody UpdateAllianceApprovalSettingsRequest request) {
    Alliance alliance = allianceService.updateAllianceApprovalSettings(request);
    return ApiResponse.success("联盟审核设置更新成功", alliance);
  }

  @PutMapping("/war-limits")
  @Operation(summary = "更新官渡战事人数上限")
  public ApiResponse<Alliance> updateWarLimits(
      @Valid @RequestBody UpdateWarLimitsRequest request) {
    Alliance alliance = allianceService.updateWarLimits(request);
    return ApiResponse.success("官渡战事人数上限更新成功", alliance);
  }

  @PutMapping("/{allianceId}/guandu-registration-time")
  @Operation(summary = "设置官渡报名时间")
  public ApiResponse<Alliance> updateGuanduRegistrationTime(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId,
      @Valid @RequestBody UpdateGuanduRegistrationTimeRequest request) {
    Alliance alliance = allianceService.updateGuanduRegistrationTime(allianceId, request);
    return ApiResponse.success("官渡报名时间设置成功", alliance);
  }

  @DeleteMapping("/{allianceId}/guandu-registration-time")
  @Operation(summary = "清除官渡报名时间设置")
  public ApiResponse<Alliance> clearGuanduRegistrationTime(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId) {
    Alliance alliance = allianceService.clearGuanduRegistrationTime(allianceId);
    return ApiResponse.success("官渡报名时间设置已清除", alliance);
  }
}
