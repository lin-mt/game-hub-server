package com.app.gamehub.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "alliances")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Alliance extends BaseEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "code", unique = true, nullable = false, length = 6)
  private String code;

  @Column(name = "server_id", nullable = false)
  private Integer serverId;

  @Column(name = "leader_id", nullable = false)
  private Long leaderId;

  @Column(name = "alliance_join_approval_required", nullable = false)
  private Boolean allianceJoinApprovalRequired = false;

  @Column(name = "war_join_approval_required", nullable = false)
  private Boolean warJoinApprovalRequired = false;

  // Replaced previous aggregate guanduOneLimit/guanduTwoLimit with separate main and substitute limits
  @Column(name = "guandu_one_main_limit", nullable = false)
  private Integer guanduOneMainLimit = 30;

  @Column(name = "guandu_one_sub_limit", nullable = false)
  private Integer guanduOneSubLimit = 10;

  @Column(name = "guandu_two_main_limit", nullable = false)
  private Integer guanduTwoMainLimit = 30;

  @Column(name = "guandu_two_sub_limit", nullable = false)
  private Integer guanduTwoSubLimit = 10;

  @Column(name = "guandu_reminder", columnDefinition = "TEXT")
  private String guanduReminder;

  @Column(name = "yy_channel_id")
  private String yyChannelId;

  @Column(name = "tencent_meeting_code")
  private String tencentMeetingCode;

  @Column(name = "tencent_meeting_password")
  private String tencentMeetingPassword;

  @Column(name = "carriage_time")
  private String carriageTime;

  // 官渡报名时间设置
  @Column(name = "guandu_registration_start_day")
  private Integer guanduRegistrationStartDay; // 1-7 表示星期一到星期日

  @Column(name = "guandu_registration_start_minute")
  private Integer guanduRegistrationStartMinute; // 0-1439 表示一天中的分钟数 (0=00:00, 1439=23:59)

  @Column(name = "guandu_registration_end_day")
  private Integer guanduRegistrationEndDay; // 1-7 表示星期一到星期日

  @Column(name = "guandu_registration_end_minute")
  private Integer guanduRegistrationEndMinute; // 0-1439 表示一天中的分钟数

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "leader_id", insertable = false, updatable = false)
  private User leader;
}
