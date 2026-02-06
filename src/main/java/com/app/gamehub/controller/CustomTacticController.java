package com.app.gamehub.controller;

import com.app.gamehub.dto.ApiResponse;
import com.app.gamehub.dto.CreateCustomTacticRequest;
import com.app.gamehub.dto.CustomTacticResponse;
import com.app.gamehub.dto.UpdateCustomTacticRequest;
import com.app.gamehub.entity.WarType;
import com.app.gamehub.service.CustomTacticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/custom-tactics")
@Tag(name = "自定义战术", description = "官渡自定义战术管理")
public class CustomTacticController {

  private final CustomTacticService customTacticService;

  public CustomTacticController(CustomTacticService customTacticService) {
    this.customTacticService = customTacticService;
  }

  @PostMapping
  @Operation(summary = "创建自定义战术")
  public ApiResponse<CustomTacticResponse> create(
      @Valid @RequestBody CreateCustomTacticRequest request) {
    CustomTacticResponse response = customTacticService.create(request);
    return ApiResponse.success("自定义战术创建成功", response);
  }

  @PutMapping("/{id}")
  @Operation(summary = "更新自定义战术")
  public ApiResponse<CustomTacticResponse> update(
      @Parameter(description = "战术ID", example = "1") @PathVariable Long id,
      @Valid @RequestBody UpdateCustomTacticRequest request) {
    CustomTacticResponse response = customTacticService.update(id, request);
    return ApiResponse.success("自定义战术更新成功", response);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "删除自定义战术")
  public ApiResponse<Void> delete(
      @Parameter(description = "战术ID", example = "1") @PathVariable Long id) {
    customTacticService.delete(id);
    return ApiResponse.success("自定义战术删除成功", null);
  }

  @GetMapping("/{id}")
  @Operation(summary = "获取自定义战术详情")
  public ApiResponse<CustomTacticResponse> detail(
      @Parameter(description = "战术ID", example = "1") @PathVariable Long id) {
    CustomTacticResponse response = customTacticService.getDetail(id);
    return ApiResponse.success(response);
  }

  @GetMapping
  @Operation(summary = "获取自定义战术列表")
  public ApiResponse<List<CustomTacticResponse>> list(
      @Parameter(description = "联盟ID", example = "1") @RequestParam Long allianceId,
      @Parameter(description = "战事类型", example = "GUANDU_ONE") @RequestParam WarType warType) {
    List<CustomTacticResponse> response = customTacticService.list(allianceId, warType);
    return ApiResponse.success(response);
  }
}
