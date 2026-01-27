package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "从文本批量更新联盟成员信息请求")
public class BulkUpdateMembersFromTextRequest {

  @NotBlank(message = "文本内容不能为空")
  @Schema(description = "成员信息的原始文本（多行）", example = "...")
  private String rawText;
}

