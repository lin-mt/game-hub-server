package com.app.gamehub.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User extends BaseEntity {

  @Column(name = "openid", unique = true, nullable = false)
  private String openid;

  @Column(name = "nickname")
  private String nickname;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(name = "phone")
  private String phone;

  @Column(name = "message_subscription_count", nullable = false)
  private Integer messageSubscriptionCount = 0;

  @Column(name = "arena_notification_enabled", nullable = false)
  private Boolean arenaNotificationEnabled = false;
}
