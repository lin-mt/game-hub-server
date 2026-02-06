package com.app.gamehub.service;

import com.app.gamehub.dto.CreateCustomTacticRequest;
import com.app.gamehub.dto.CustomTacticConfig;
import com.app.gamehub.dto.CustomTacticResponse;
import com.app.gamehub.dto.UpdateCustomTacticRequest;
import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.TacticTemplate;
import com.app.gamehub.entity.WarType;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.AllianceRepository;
import com.app.gamehub.repository.TacticTemplateRepository;
import com.app.gamehub.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomTacticService {

  private static final String TEMPLATE_TYPE_CUSTOM = "CUSTOM";

  private final TacticTemplateRepository tacticTemplateRepository;
  private final AllianceRepository allianceRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public CustomTacticResponse create(CreateCustomTacticRequest request) {
    Alliance alliance = validateLeader(request.getAllianceId());
    if (!WarType.isGuanDu(request.getWarType())) {
      throw new BusinessException("自定义战术仅支持官渡战事");
    }

    TacticTemplate template = new TacticTemplate();
    template.setAllianceId(alliance.getId());
    template.setWarType(request.getWarType());
    template.setTemplateType(TEMPLATE_TYPE_CUSTOM);
    template.setName(request.getName().trim());
    template.setTacticKey(buildTacticKey(alliance.getId(), request.getWarType().name()));

    CustomTacticConfig config = new CustomTacticConfig();
    config.setGroups(request.getGroups() != null ? request.getGroups() : new ArrayList<>());
    template.setConfigJson(writeConfig(config));

    tacticTemplateRepository.save(template);
    return toResponse(template, config);
  }

  @Transactional
  public CustomTacticResponse update(Long id, UpdateCustomTacticRequest request) {
    TacticTemplate template = getTemplateOrThrow(id);
    validateLeader(template.getAllianceId());
    if (!WarType.isGuanDu(template.getWarType())) {
      throw new BusinessException("自定义战术仅支持官渡战事");
    }

    template.setName(request.getName().trim());
    CustomTacticConfig config = new CustomTacticConfig();
    config.setGroups(request.getGroups() != null ? request.getGroups() : new ArrayList<>());
    template.setConfigJson(writeConfig(config));

    tacticTemplateRepository.save(template);
    return toResponse(template, config);
  }

  @Transactional
  public void delete(Long id) {
    TacticTemplate template = getTemplateOrThrow(id);
    validateLeader(template.getAllianceId());
    tacticTemplateRepository.delete(template);
  }

  public CustomTacticResponse getDetail(Long id) {
    TacticTemplate template = getTemplateOrThrow(id);
    validateLeader(template.getAllianceId());
    CustomTacticConfig config = readConfig(template.getConfigJson());
    return toResponse(template, config);
  }

  public List<CustomTacticResponse> list(Long allianceId, WarType warType) {
    validateLeader(allianceId);
    if (!WarType.isGuanDu(warType)) {
      throw new BusinessException("自定义战术仅支持官渡战事");
    }
    List<TacticTemplate> templates =
        tacticTemplateRepository.findByAllianceIdAndWarTypeAndTemplateTypeOrderByUpdatedAtDesc(
            allianceId, warType, TEMPLATE_TYPE_CUSTOM);
    List<CustomTacticResponse> results = new ArrayList<>();
    for (TacticTemplate template : templates) {
      CustomTacticConfig config = readConfig(template.getConfigJson());
      results.add(toResponse(template, config));
    }
    return results;
  }

  private Alliance validateLeader(Long allianceId) {
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));
    Long userId = UserContext.getUserId();
    if (userId == null || !userId.equals(alliance.getLeaderId())) {
      throw new BusinessException("只有盟主可以管理自定义战术");
    }
    return alliance;
  }

  private TacticTemplate getTemplateOrThrow(Long id) {
    TacticTemplate template =
        tacticTemplateRepository.findById(id).orElseThrow(() -> new BusinessException("战术不存在"));
    if (!TEMPLATE_TYPE_CUSTOM.equals(template.getTemplateType())) {
      throw new BusinessException("战术类型不支持");
    }
    return template;
  }

  private CustomTacticResponse toResponse(TacticTemplate template, CustomTacticConfig config) {
    CustomTacticResponse resp = new CustomTacticResponse();
    resp.setId(template.getId());
    resp.setAllianceId(template.getAllianceId());
    resp.setWarType(template.getWarType());
    resp.setName(template.getName());
    resp.setGroups(config != null ? config.getGroups() : null);
    resp.setCreatedAt(template.getCreatedAt());
    resp.setUpdatedAt(template.getUpdatedAt());
    return resp;
  }

  private String writeConfig(CustomTacticConfig config) {
    try {
      return objectMapper.writeValueAsString(config);
    } catch (Exception ex) {
      throw new BusinessException("战术配置保存失败");
    }
  }

  private CustomTacticConfig readConfig(String json) {
    if (json == null || json.isBlank()) {
      CustomTacticConfig empty = new CustomTacticConfig();
      empty.setGroups(new ArrayList<>());
      return empty;
    }
    try {
      CustomTacticConfig cfg = objectMapper.readValue(json, CustomTacticConfig.class);
      if (cfg.getGroups() == null) {
        cfg.setGroups(new ArrayList<>());
      }
      return cfg;
    } catch (Exception ex) {
      CustomTacticConfig empty = new CustomTacticConfig();
      empty.setGroups(new ArrayList<>());
      return empty;
    }
  }

  private String buildTacticKey(Long allianceId, String warType) {
    return "CUSTOM_" + allianceId + "_" + warType + "_" + UUID.randomUUID();
  }
}
