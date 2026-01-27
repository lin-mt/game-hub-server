package com.app.gamehub.controller;

import com.app.gamehub.dto.ApiResponse;
import com.app.gamehub.dto.CarriageQueueListResponse;
import com.app.gamehub.dto.InsertMemberRequest;
import com.app.gamehub.dto.JoinCarriageQueueRequest;
import com.app.gamehub.dto.LeaveCarriageQueueRequest;
import com.app.gamehub.dto.RemoveMemberRequest;
import com.app.gamehub.dto.SetTodayDriverRequest;
import com.app.gamehub.entity.CarriageQueue;
import com.app.gamehub.service.CarriageQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carriage")
@Tag(name = "联盟马车", description = "联盟马车排队相关接口")
@RequiredArgsConstructor
public class CarriageController {

  private final CarriageQueueService carriageQueueService;

  @PostMapping("/join")
  @Operation(summary = "报名马车", description = "联盟成员报名马车，加入排队列表末尾")
  public ApiResponse<CarriageQueue> joinQueue(
      @Valid @RequestBody JoinCarriageQueueRequest request) {
    CarriageQueue queue = carriageQueueService.joinQueue(request);
    return ApiResponse.success("报名成功", queue);
  }

  @PostMapping("/leave")
  @Operation(summary = "退出马车队列", description = "从马车排队列表中退出")
  public ApiResponse<Void> leaveQueue(@Valid @RequestBody LeaveCarriageQueueRequest request) {
    carriageQueueService.leaveQueue(request);
    return ApiResponse.success("退出成功", null);
  }

  @GetMapping("/queue/{allianceId}")
  @Operation(summary = "查询马车排队列表", description = "查询指定联盟的马车排队列表，标记今日开车的人")
  public ApiResponse<CarriageQueueListResponse> getQueueList(
      @Parameter(description = "联盟ID", example = "1") @PathVariable Long allianceId) {
    CarriageQueueListResponse response = carriageQueueService.getQueueList(allianceId);
    return ApiResponse.success(response);
  }

  @PostMapping("/set-driver")
  @Operation(summary = "设置今日车主", description = "选择联盟成员设置为今日车主，如果今日已有车主则将原车主插入到队伍第一名")
  public ApiResponse<Void> setTodayDriver(@Valid @RequestBody SetTodayDriverRequest request) {
    carriageQueueService.setTodayDriver(request.getAllianceId(), request.getAccountId());
    return ApiResponse.success("设置成功", null);
  }

  @PostMapping("/remove")
  @Operation(summary = "移除队伍成员", description = "从马车队伍中移除指定成员")
  public ApiResponse<Void> removeMember(@Valid @RequestBody RemoveMemberRequest request) {
    carriageQueueService.removeMember(request.getAllianceId(), request.getAccountId());
    return ApiResponse.success("移除成功", null);
  }

  @PostMapping("/insert")
  @Operation(summary = "插队添加成员", description = "在马车队伍中指定位置插入成员")
  public ApiResponse<Void> insertMember(@Valid @RequestBody InsertMemberRequest request) {
    carriageQueueService.insertMember(
        request.getAllianceId(), request.getAccountId(), request.getPosition());
    return ApiResponse.success("插入成功", null);
  }
}
