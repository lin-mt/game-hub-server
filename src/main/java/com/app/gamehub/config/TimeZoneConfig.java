package com.app.gamehub.config;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 时区配置类
 * 确保应用程序使用中国时区（GMT+8）
 */
@Slf4j
@Configuration
public class TimeZoneConfig {

    @PostConstruct
    public void init() {
        // 设置JVM默认时区为中国时区
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        log.info("应用程序时区已设置为: {}", TimeZone.getDefault().getID());
        log.info("当前时区偏移量: {} 小时", TimeZone.getDefault().getRawOffset() / (1000 * 60 * 60));
    }
}
