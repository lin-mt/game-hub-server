package com.app.gamehub.service;

import com.app.gamehub.dto.LoginRequest;
import com.app.gamehub.dto.LoginResponse;
import com.app.gamehub.dto.UserInfoResponse;
import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.Dynasty;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.entity.User;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.AllianceRepository;
import com.app.gamehub.repository.DynastyRepository;
import com.app.gamehub.repository.GameAccountRepository;
import com.app.gamehub.repository.UserRepository;
import com.app.gamehub.util.JwtUtil;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final AllianceRepository allianceRepository;
  private final DynastyRepository dynastyRepository;
  private final GameAccountRepository gameAccountRepository;
  private final JwtUtil jwtUtil;
  private final WeChatService weChatService;

  public AuthService(
      WeChatService weChatService,
      UserRepository userRepository,
      AllianceRepository allianceRepository,
      DynastyRepository dynastyRepository,
      GameAccountRepository gameAccountRepository,
      JwtUtil jwtUtil) {
    this.weChatService = weChatService;
    this.userRepository = userRepository;
    this.allianceRepository = allianceRepository;
    this.dynastyRepository = dynastyRepository;
    this.gameAccountRepository = gameAccountRepository;
    this.jwtUtil = jwtUtil;
  }

  @Transactional
  public LoginResponse login(LoginRequest request) {
    try {
      // 调用微信接口获取openid
      String openid = weChatService.getOpenidByCode(request.getCode());

      if (openid == null) {
        throw new BusinessException("获取用户openid失败");
      }

      // 查找或创建用户
      User user = userRepository.findByOpenid(openid).orElse(null);
      boolean isNewUser = false;

      if (user == null) {
        user = new User();
        user.setOpenid(openid);
        user.setNickname(request.getNickname());
        user.setAvatarUrl(request.getAvatarUrl());
        user = userRepository.save(user);
        isNewUser = true;
      } else {
        // 更新用户信息
        if (request.getNickname() != null) {
          user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
          user.setAvatarUrl(request.getAvatarUrl());
        }
        user = userRepository.save(user);
      }

      // 生成JWT token
      String token = jwtUtil.generateToken(user.getId(), user.getOpenid());

      // 构建响应
      LoginResponse response = new LoginResponse();
      response.setToken(token);
      response.setUserId(user.getId().toString());
      response.setNickname(user.getNickname());
      response.setAvatarUrl(user.getAvatarUrl());
      response.setIsNewUser(isNewUser);

      return response;

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("用户登录失败", e);
      throw new BusinessException("登录失败: " + e.getMessage());
    }
  }

  public User getUserInfo(Long userId) {
    return userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
  }

  public UserInfoResponse getUserCompleteInfo(Long userId) {
    User user = getUserInfo(userId);
    List<Alliance> alliances = allianceRepository.findByLeaderIdOrderByServerIdDesc(userId);
    List<Dynasty> dynasties = dynastyRepository.findByEmperorIdOrderByServerIdDesc(userId);
    List<GameAccount> gameAccounts = gameAccountRepository.findByUserIdOrderByServerIdDesc(userId);

    UserInfoResponse response = new UserInfoResponse();
    response.setUser(user);
    response.setAlliances(alliances);
    response.setDynasties(dynasties);
    response.setGameAccounts(gameAccounts);

    return response;
  }
}
