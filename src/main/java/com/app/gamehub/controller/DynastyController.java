package com.app.gamehub.controller;

import com.app.gamehub.dto.*;
import com.app.gamehub.entity.Dynasty;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.entity.PositionType;
import com.app.gamehub.service.DynastyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 王朝控制器 */
@RestController
@RequestMapping("/api/dynasties")
@Tag(name = "王朝管理", description = "王朝相关接口")
public class DynastyController {

  private final DynastyService dynastyService;

  public DynastyController(DynastyService dynastyService) {
    this.dynastyService = dynastyService;
  }

  @PostMapping
  @Operation(summary = "创建王朝")
  public ApiResponse<Dynasty> createDynasty(@Valid @RequestBody CreateDynastyRequest request) {
    Dynasty dynasty = dynastyService.createDynasty(request);
    return ApiResponse.success("王朝创建成功", dynasty);
  }

  @PutMapping("/{dynastyId}")
  @Operation(summary = "更新王朝")
  public ApiResponse<Dynasty> updateDynasty(
      @Valid @RequestBody UpdateDynastyRequest request,
      @Parameter(description = "王朝ID", example = "1") @PathVariable Long dynastyId) {
    Dynasty dynasty = dynastyService.updateDynasty(dynastyId, request);
    return ApiResponse.success("王朝信息更新成功", dynasty);
  }

  @DeleteMapping("/{dynastyId}")
  @Operation(summary = "删除王朝")
  public ApiResponse<Void> deleteDynasty(
      @Parameter(description = "王朝ID", example = "1") @PathVariable Long dynastyId) {
    dynastyService.deleteDynasty(dynastyId);
    return ApiResponse.success("王朝删除成功", null);
  }

  @GetMapping("/my")
  @Operation(summary = "获取我创建的王朝列表")
  public ApiResponse<List<Dynasty>> getMyDynasties() {
    List<Dynasty> dynasties = dynastyService.getUserDynasties();
    return ApiResponse.success(dynasties);
  }

  @GetMapping("/{dynastyId}")
  @Operation(summary = "获取王朝详情")
  public ApiResponse<DynastyDetailResponse> getDynastyDetail(
      @Parameter(description = "王朝ID", example = "1") @PathVariable Long dynastyId) {
    DynastyDetailResponse detail = dynastyService.getDynastyDetail(dynastyId);
    return ApiResponse.success(detail);
  }

  @GetMapping("/code/{code}")
  @Operation(summary = "通过编码获取王朝信息")
  public ApiResponse<Dynasty> getDynastyByCode(
      @Parameter(description = "王朝编码", example = "ABC123") @PathVariable String code) {
    Dynasty dynasty = dynastyService.getDynastyByCode(code);
    return ApiResponse.success(dynasty);
  }

  @PostMapping("/{dynastyId}/toggle-reservation")
  @Operation(summary = "开启或关闭官职预约")
  public ApiResponse<Dynasty> toggleReservationEnabled(
      @Parameter(description = "王朝ID", example = "1") @PathVariable Long dynastyId,
      @Parameter(description = "是否开启", example = "true") @RequestParam boolean enabled) {
    Dynasty dynasty = dynastyService.toggleReservationEnabled(dynastyId, enabled);
    return ApiResponse.success("官职预约状态已更新", dynasty);
  }

  @PostMapping("/{dynastyId}/toggle-auto-config")
  @Operation(summary = "开启或关闭自动配置官职预约")
  public ApiResponse<Dynasty> toggleAutoConfigureReservation(
      @Parameter(description = "王朝ID", example = "1") @PathVariable Long dynastyId,
      @Parameter(description = "是否开启", example = "true") @RequestParam boolean enabled) {
    Dynasty dynasty = dynastyService.toggleAutoConfigureReservation(dynastyId, enabled);
    return ApiResponse.success("自动配置官职预约状态已更新", dynasty);
  }

  @DeleteMapping("/{dynastyId}/reservation-results")
  @Operation(summary = "清空所有预约结果")
  public ApiResponse<Void> clearAllReservationResults(
      @Parameter(description = "王朝ID", example = "1") @PathVariable Long dynastyId) {
    dynastyService.clearAllReservationResults(dynastyId);
    return ApiResponse.success("已清空所有预约结果", null);
  }

  @DeleteMapping("/{dynastyId}/reservation-results/{positionType}")
  @Operation(summary = "清空指定官职的预约结果")
  public ApiResponse<Void> clearPositionReservationResults(
      @Parameter(description = "王朝ID", example = "1") @PathVariable Long dynastyId,
      @Parameter(description = "官职类型", example = "TAI_WEI") @PathVariable
          PositionType positionType) {
    dynastyService.clearPositionReservationResults(dynastyId, positionType);
    return ApiResponse.success("已清空指定官职的预约结果", null);
  }

  @PostMapping("/accounts/{accountId}/leave")
  @Operation(summary = "账号退出王朝")
  public ApiResponse<GameAccount> leaveDynasty(
      @Parameter(description = "账号ID", example = "1") @PathVariable Long accountId) {
    GameAccount account = dynastyService.leaveDynasty(accountId);
    return ApiResponse.success("账号退出王朝成功", account);
  }
}
