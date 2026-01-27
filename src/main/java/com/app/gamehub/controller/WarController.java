package com.app.gamehub.controller;

import com.app.gamehub.dto.ApiResponse;
import com.app.gamehub.dto.ProcessApplicationRequest;
import com.app.gamehub.dto.UseTacticRequest;
import com.app.gamehub.dto.WarLimitStatusResponse;
import com.app.gamehub.dto.WarRequest;
import com.app.gamehub.entity.WarApplication;
import com.app.gamehub.entity.WarArrangement;
import com.app.gamehub.entity.WarType;
import com.app.gamehub.service.WarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wars")
@Tag(name = "战事管理", description = "战事相关接口")
public class WarController {

  @Autowired private WarService warService;

  @PostMapping("/apply")
  @Operation(summary = "申请参加战事")
  public ApiResponse<WarApplication> applyForWar(@Valid @RequestBody WarRequest request) {
    WarApplication application = warService.applyForWar(request);
    return ApiResponse.success("战事申请提交成功", application);
  }

  @PostMapping("/cancel-apply")
  @Operation(summary = "取消申请参加战事")
  public ApiResponse<Void> cancelApplyForWar(@Valid @RequestBody WarRequest request) {
    warService.cancelApplyForWar(request);
    return ApiResponse.success("战事取消申请成功", null);
  }

  @PostMapping("/applications/{applicationId}/process")
  @Operation(summary = "处理战事申请")
  public ApiResponse<WarApplication> processWarApplication(
      @Parameter(description = "申请ID", example = "1") @PathVariable Long applicationId,
      @Valid @RequestBody ProcessApplicationRequest request) {
    WarApplication application =
        warService.processWarApplication(applicationId, request.getApproved());
    return ApiResponse.success("战事申请处理成功", application);
  }

  @PostMapping("/move-guan-du/{accountId}")
  @Operation(summary = "调动成员参加的战事（官渡一与官渡二之间互相调动）")
  public ApiResponse<WarArrangement> moveWarApplication(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId) {
    WarArrangement war = warService.moveGuanDuWar(accountId);
    return ApiResponse.success("战事申请移动成功", war);
  }

  @PostMapping("/add-to-war/{accountId}")
  @Operation(summary = "盟主直接添加成员到战事中")
  public ApiResponse<WarArrangement> addToWar(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId,
      @Parameter(description = "战事类型") @RequestParam WarType warType,
      @Parameter(description = "是否替补") @RequestParam Boolean isSubstitute) {
    WarArrangement war = warService.addToWar(accountId, warType, isSubstitute);
    return ApiResponse.success("添加成功", war);
  }

  @PostMapping("/remove-from-war/{accountId}")
  @Operation(summary = "将成员移除出指定战事")
  public ApiResponse<Object> removeFromWar(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId,
      @Parameter(description = "战事类型") @RequestParam WarType warType) {
    warService.removeFromWar(accountId, warType);
    return ApiResponse.success("移除成功", null);
  }

  @GetMapping("/alliances/{allianceId}/applications")
  @Operation(summary = "获取联盟战事申请列表")
  public ApiResponse<List<WarApplication>> getPendingWarApplications(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId,
      @Parameter(description = "战事类型") @RequestParam WarType warType) {
    List<WarApplication> applications = warService.getPendingWarApplications(allianceId, warType);
    return ApiResponse.success(applications);
  }

  @GetMapping("/accounts/{accountId}/applications")
  @Operation(summary = "获取账号战事申请历史")
  public ApiResponse<List<WarApplication>> getAccountWarApplications(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId) {
    List<WarApplication> applications = warService.getAccountWarApplications(accountId);
    return ApiResponse.success(applications);
  }

  @PostMapping("/use-tactic/{allianceId}")
  @Operation(summary = "联盟使用预制战术")
  public ApiResponse<Void> useTactic(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId,
      @Valid @RequestBody UseTacticRequest request) {
    warService.useTactic(allianceId, request);
    return ApiResponse.success(null);
  }

  @GetMapping("/alliance/{allianceId}/limit-status")
  @Operation(summary = "获取联盟官渡战事人数上限状态")
  public ApiResponse<WarLimitStatusResponse> getWarLimitStatus(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId) {
    WarLimitStatusResponse status = warService.getWarLimitStatus(allianceId);
    return ApiResponse.success(status);
  }

  @GetMapping("/alliances/{allianceId}/export-personnel")
  @Operation(summary = "导出联盟战事人员名单")
  public void exportWarPersonnel(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId,
      HttpServletResponse response) throws IOException {
    warService.exportWarPersonnel(allianceId, response);
  }
}
