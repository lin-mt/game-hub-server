package com.app.gamehub.controller;

import com.app.gamehub.dto.ApiResponse;
import com.app.gamehub.util.LvbuStarLevelUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lvbu-star-levels")
@Tag(name = "吕布星级管理", description = "吕布星级相关接口")
@RequiredArgsConstructor
public class LvbuStarLevelController {

  @GetMapping("/valid-levels")
  @Operation(summary = "获取所有有效的吕布星级值")
  public ApiResponse<List<BigDecimal>> getValidStarLevels() {
    List<BigDecimal> validLevels = LvbuStarLevelUtil.getAllValidStarLevels();
    return ApiResponse.success(validLevels);
  }
}
