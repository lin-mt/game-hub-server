package com.app.gamehub.controller;

import com.app.gamehub.dto.ApiResponse;
import com.app.gamehub.dto.AllianceMemberSummaryDto;
import com.app.gamehub.dto.BulkUpdateMembersFromTextRequest;
import com.app.gamehub.dto.JoinAllianceRequest;
import com.app.gamehub.dto.ProcessApplicationRequest;
import com.app.gamehub.entity.AllianceApplication;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.service.AllianceMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alliance-members")
@Tag(name = "联盟成员管理", description = "联盟成员相关接口")
public class AllianceMemberController {

  private final AllianceMemberService allianceMemberService;

  public AllianceMemberController(AllianceMemberService allianceMemberService) {
    this.allianceMemberService = allianceMemberService;
  }

  @PostMapping("/apply")
  @Operation(summary = "申请加入联盟")
  public ApiResponse<AllianceApplication> applyToJoinAlliance(
      @Valid @RequestBody JoinAllianceRequest request) {
    AllianceApplication application = allianceMemberService.applyToJoinAlliance(request);
    return ApiResponse.success("申请提交成功", application);
  }

  @PostMapping("/applications/{applicationId}/process")
  @Operation(summary = "处理加入联盟申请")
  public ApiResponse<AllianceApplication> processApplication(
      @Parameter(description = "申请ID", example = "1") @PathVariable Long applicationId,
      @Valid @RequestBody ProcessApplicationRequest request) {

    AllianceApplication application =
        allianceMemberService.processApplication(applicationId, request.getApproved());
    return ApiResponse.success("申请处理成功", application);
  }

  @DeleteMapping("/accounts/{accountId}")
  @Operation(summary = "移除联盟成员")
  public ApiResponse<Void> removeMember(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId) {
    allianceMemberService.removeMember(accountId);
    return ApiResponse.success("成员移除成功", null);
  }

  @GetMapping("/alliances/{allianceId}/applications")
  @Operation(summary = "获取联盟待处理申请列表")
  public ApiResponse<List<AllianceApplication>> getPendingApplications(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId) {
    List<AllianceApplication> applications =
        allianceMemberService.getPendingApplications(allianceId);
    return ApiResponse.success(applications);
  }

  @GetMapping("/accounts/{accountId}/applications")
  @Operation(summary = "获取账号申请历史")
  public ApiResponse<List<AllianceApplication>> getAccountApplications(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId) {
    List<AllianceApplication> applications =
        allianceMemberService.getAccountApplications(accountId);
    return ApiResponse.success(applications);
  }

  @GetMapping("/alliances/{allianceId}/members")
  @Operation(summary = "获取联盟成员列表")
  public ApiResponse<List<AllianceMemberSummaryDto>> getAllianceMembers(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId) {
    List<AllianceMemberSummaryDto> members = allianceMemberService.getAllianceMembers(allianceId);
    return ApiResponse.success(members);
  }

  @PostMapping("/alliances/{allianceId}/bulk-update-from-text")
  @Operation(summary = "从文本批量更新联盟成员信息（阶级和战力）")
  public ApiResponse<String> bulkUpdateFromText(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId,
      @Valid @RequestBody BulkUpdateMembersFromTextRequest request) {
    String result = allianceMemberService.bulkUpdateMembersFromText(
        allianceId, request.getRawText(), request.getRemoveMissing());
    return ApiResponse.success("批量更新完成", result);
  }

  @GetMapping("/alliances/{allianceId}/export")
  @Operation(summary = "导出联盟成员列表")
  public void exportMembers(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId,
      HttpServletResponse response) throws IOException {
    allianceMemberService.exportMembers(allianceId, response);
  }

  @GetMapping("/alliances/{allianceId}/unowned-accounts")
  @Operation(summary = "获取联盟中的无主账号列表")
  public ApiResponse<List<GameAccount>> getUnownedAccounts(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId) {
    List<GameAccount> unownedAccounts = allianceMemberService.getUnownedAccounts(allianceId);
    return ApiResponse.success(unownedAccounts);
  }
}
