package com.app.gamehub.service;

import com.app.gamehub.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatService {

  private static final String HTTPS_JSCODE2SESSION_URL =
      "https://api.weixin.qq.com/sns/jscode2session";
  private static final String HTTP_JSCODE2SESSION_URL =
      "https://api.weixin.qq.com/sns/jscode2session";

  private static final String HTTPS_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
  private static final String HTTP_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";

  private static final String HTTPS_SEND_MESSAGE_URL =
      "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";
  private static final String HTTP_SEND_MESSAGE_URL =
      "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";

  private final ObjectMapper objectMapper;
  private final Environment environment;

  @Value("${wechat.miniapp.appid}")
  private String appId;

  @Value("${wechat.miniapp.secret}")
  private String secret;

  /**
   * 通过微信小程序登录凭证获取用户openid
   *
   * @param code 微信小程序登录凭证
   * @return 用户openid
   */
  public String getOpenidByCode(String code) {
    // 根据环境选择协议：生产环境使用HTTP，其他环境使用HTTPS
    boolean isProd = isProdEnvironment();
    String baseUrl = isProd ? HTTP_JSCODE2SESSION_URL : HTTPS_JSCODE2SESSION_URL;

    log.debug("当前环境: {}, 使用协议: {}", isProd ? "生产环境" : "非生产环境", isProd ? "HTTP" : "HTTPS");

    // 构建请求URL
    String url =
        String.format(
            "%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            baseUrl, appId, secret, code);

    // 创建HttpClient
    try (HttpClient client =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
      // 创建请求
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .timeout(Duration.ofSeconds(30))
              .GET()
              .build();

      // 发送请求
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        log.error("微信接口调用失败，状态码: {}, 响应: {}", response.statusCode(), response.body());
        throw new BusinessException("微信接口调用失败");
      }

      // 解析响应
      WeChatSessionResponse sessionResponse =
          objectMapper.readValue(response.body(), WeChatSessionResponse.class);

      if (sessionResponse.getErrcode() != null && sessionResponse.getErrcode() != 0) {
        log.error(
            "微信接口返回错误，错误码: {}, 错误信息: {}",
            sessionResponse.getErrcode(),
            sessionResponse.getErrmsg());
        throw new BusinessException("微信登录失败: " + sessionResponse.getErrmsg());
      }

      if (sessionResponse.getOpenid() == null || sessionResponse.getOpenid().isEmpty()) {
        log.error("微信接口返回的openid为空");
        throw new BusinessException("获取用户openid失败");
      }

      log.info("成功获取用户openid: {}", sessionResponse.getOpenid());
      return sessionResponse.getOpenid();

    } catch (IOException e) {
      log.error("解析微信接口响应失败", e);
      throw new BusinessException("解析微信接口响应失败");
    } catch (InterruptedException e) {
      log.error("微信接口调用被中断", e);
      Thread.currentThread().interrupt();
      throw new BusinessException("微信接口调用被中断");
    } catch (Exception e) {
      log.error("调用微信接口异常", e);
      throw new BusinessException("调用微信接口异常: " + e.getMessage());
    }
  }

  /**
   * 发送微信订阅消息
   *
   * @param openid 用户openid
   * @param templateId 模板ID
   * @param templateData 模板数据
   */
  public void sendSubscribeMessage(
      String openid, String templateId, Map<String, Object> templateData, String page) {
    // 创建HttpClient
    try (HttpClient client =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
      // 获取access_token
      String accessToken = getAccessToken();

      // 根据环境选择协议
      boolean isProd = isProdEnvironment();
      String sendMessageUrl = isProd ? HTTP_SEND_MESSAGE_URL : HTTPS_SEND_MESSAGE_URL;

      // 构建请求URL
      String url = sendMessageUrl + "?access_token=" + accessToken;

      // 构建请求体
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("touser", openid);
      requestBody.put("template_id", templateId);
      requestBody.put("data", templateData);
      if (StringUtils.isNotBlank(page)) {
        requestBody.put("page", page);
      }

      String requestBodyJson = objectMapper.writeValueAsString(requestBody);
      // 创建请求
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .timeout(Duration.ofSeconds(30))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
              .build();

      // 发送请求
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        log.error("发送微信消息失败，状态码: {}, 响应: {}", response.statusCode(), response.body());
        throw new BusinessException("发送微信消息失败");
      }

      // 解析响应
      @SuppressWarnings("unchecked")
      Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
      Integer errcode = (Integer) responseMap.get("errcode");

      if (errcode != null && errcode != 0) {
        String errmsg = (String) responseMap.get("errmsg");
        log.error("微信消息发送失败，错误码: {}, 错误信息: {}", errcode, errmsg);
        throw new BusinessException("微信消息发送失败: " + errmsg);
      }

    } catch (Exception e) {
      log.error("发送微信消息异常，用户: {}, 模板: {}, 错误: {}", openid, templateId, e.getMessage());
      throw new BusinessException("发送微信消息异常: " + e.getMessage());
    }
  }

  /**
   * 获取微信access_token
   *
   * @return access_token
   */
  private String getAccessToken() {
    // 根据环境选择协议
    boolean isProd = isProdEnvironment();
    String tokenUrl = isProd ? HTTP_ACCESS_TOKEN_URL : HTTPS_ACCESS_TOKEN_URL;

    // 构建请求URL
    String url =
        String.format(
            "%s?grant_type=client_credential&appid=%s&secret=%s", tokenUrl, appId, secret);

    // 创建HttpClient
    try (HttpClient client =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {

      // 创建请求
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .timeout(Duration.ofSeconds(30))
              .GET()
              .build();

      // 发送请求
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        log.error("获取access_token失败，状态码: {}, 响应: {}", response.statusCode(), response.body());
        throw new BusinessException("获取access_token失败");
      }

      // 解析响应
      @SuppressWarnings("unchecked")
      Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);

      Integer errcode = (Integer) responseMap.get("errcode");
      if (errcode != null && errcode != 0) {
        String errmsg = (String) responseMap.get("errmsg");
        log.error("获取access_token失败，错误码: {}, 错误信息: {}", errcode, errmsg);
        throw new BusinessException("获取access_token失败: " + errmsg);
      }

      String accessToken = (String) responseMap.get("access_token");
      if (accessToken == null || accessToken.isEmpty()) {
        log.error("获取到的access_token为空");
        throw new BusinessException("获取到的access_token为空");
      }

      log.debug("成功获取access_token");
      return accessToken;

    } catch (Exception e) {
      log.error("获取access_token异常", e);
      throw new BusinessException("获取access_token异常: " + e.getMessage());
    }
  }

  /**
   * 判断是否为生产环境
   *
   * @return true if production environment, false otherwise
   */
  private boolean isProdEnvironment() {
    String[] activeProfiles = environment.getActiveProfiles();
    for (String profile : activeProfiles) {
      if ("prod".equals(profile)) {
        return true;
      }
    }
    return false;
  }

  /** 微信小程序登录接口响应 */
  @Data
  public static class WeChatSessionResponse {
    @JsonProperty("openid")
    private String openid;

    @JsonProperty("session_key")
    private String sessionKey;

    @JsonProperty("unionid")
    private String unionid;

    @JsonProperty("errcode")
    private Integer errcode;

    @JsonProperty("errmsg")
    private String errmsg;
  }
}
