package com.app.gamehub.controller;

import com.app.gamehub.dto.AllianceDetailResponse;
import com.app.gamehub.dto.ApiResponse;
import com.app.gamehub.entity.AllianceApplication;
import com.app.gamehub.entity.WarApplication;
import com.app.gamehub.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query")
@Tag(name = "查询接口", description = "各种查询相关接口")
public class QueryController {

  private final QueryService queryService;

  public QueryController(QueryService queryService) {
    this.queryService = queryService;
  }

  @GetMapping("/alliances/{allianceId}/detail")
  @Operation(summary = "获取联盟详细信息")
  public ApiResponse<AllianceDetailResponse> getAllianceDetail(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId) {
    AllianceDetailResponse detail = queryService.getAllianceDetail(allianceId);
    return ApiResponse.success(detail);
  }

  @GetMapping("/accounts/{accountId}/alliance-applications")
  @Operation(summary = "查询账号申请加入联盟的状态")
  public ApiResponse<List<AllianceApplication>> getAccountAllianceApplicationStatus(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId) {
    List<AllianceApplication> applications =
        queryService.getAccountAllianceApplicationStatus(accountId);
    return ApiResponse.success(applications);
  }

  @GetMapping("/accounts/{accountId}/war-applications")
  @Operation(summary = "查询账号申请加入战事的状态")
  public ApiResponse<List<WarApplication>> getAccountWarApplicationStatus(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId) {
    List<WarApplication> applications = queryService.getAccountWarApplicationStatus(accountId);
    return ApiResponse.success(applications);
  }

  @GetMapping("/alliances/{allianceId}/war-applications")
  @Operation(summary = "查询申请加入官渡战事的申请列表")
  public ApiResponse<Map<String, List<WarApplication>>> getAllianceWarApplications(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId) {
    Map<String, List<WarApplication>> applications =
        queryService.getAllianceWarApplications(allianceId);
    return ApiResponse.success(applications);
  }
}
