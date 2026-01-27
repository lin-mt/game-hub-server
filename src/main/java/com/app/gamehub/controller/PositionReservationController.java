package com.app.gamehub.controller;

import com.app.gamehub.dto.*;
import com.app.gamehub.entity.DynastyPosition;
import com.app.gamehub.entity.PositionReservation;
import com.app.gamehub.entity.PositionType;
import com.app.gamehub.service.PositionReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/** 官职预约控制器 */
@RestController
@RequestMapping("/api/dynasties/{dynastyId}/positions")
@Tag(name = "官职预约", description = "官职预约相关接口")
public class PositionReservationController {

  private final PositionReservationService positionReservationService;

  public PositionReservationController(PositionReservationService positionReservationService) {
    this.positionReservationService = positionReservationService;
  }

  @PostMapping("/reservation-time")
  @Operation(summary = "设置官职预约时间")
  public ApiResponse<DynastyPosition> setPositionReservationTime(
      @Parameter(description = "王朝ID", example = "1") @PathVariable Long dynastyId,
      @Valid @RequestBody SetPositionReservationTimeRequest request) {
    DynastyPosition position = positionReservationService.setPositionReservationTime(dynastyId, request);
    return ApiResponse.success("官职预约时间设置成功", position);
  }

  @GetMapping("/reservation-time")
  @Operation(summary = "查询官职预约时间的设置信息")
  public ApiResponse<DynastyPosition> getPositionReservationTime(
      @Parameter(description = "王朝ID", example = "1") @PathVariable Long dynastyId,
      @RequestParam PositionType positionType) {
    DynastyPosition position = positionReservationService.getPositionReservationTime(dynastyId, positionType);
    return ApiResponse.success("查询官职预约时间设置", position);
  }

  @PostMapping("/reserve")
  @Operation(summary = "预约官职")
  public ApiResponse<PositionReservation> reservePosition(
      @Parameter(description = "王朝ID", example = "1") @PathVariable Long dynastyId,
      @Parameter(description = "账号ID", example = "1") @RequestParam Long accountId,
      @Valid @RequestBody ReservePositionRequest request) {
    PositionReservation reservation = positionReservationService.reservePosition(dynastyId, accountId, request);
    return ApiResponse.success("官职预约成功", reservation);
  }

  @PostMapping("/renounce")
  @Operation(summary = "放弃官职")
  public ApiResponse<PositionReservation> renouncePosition(
      @Parameter(description = "王朝ID", example = "1") @PathVariable Long dynastyId,
      @Parameter(description = "账号ID", example = "1") @RequestParam Long accountId,
      @Valid @RequestBody ReservePositionRequest request) {
    PositionReservation reservation = positionReservationService.renouncePosition(dynastyId, accountId, request);
    return ApiResponse.success("官职放弃成功", reservation);
  }

  @GetMapping("/reservation-results")
  @Operation(summary = "获取官职预约结果")
  public ApiResponse<PositionReservationResultResponse> getReservationResults(
      @Parameter(description = "王朝ID", example = "1") @PathVariable Long dynastyId,
      @Parameter(description = "任职日期", example = "2025/08/12")
          @RequestParam
          @DateTimeFormat(pattern = "yyyy/MM/dd")
          LocalDate dutyDate) {
    PositionReservationResultResponse results = positionReservationService.getReservationResults(dynastyId, dutyDate);
    return ApiResponse.success(results);
  }
}