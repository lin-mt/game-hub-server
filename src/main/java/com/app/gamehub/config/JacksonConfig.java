package com.app.gamehub.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonConfig {

  @Bean
  @Primary
  public ObjectMapper jsonObjectMapper(Jackson2ObjectMapperBuilder builder) {
    ObjectMapper objectMapper = builder.createXmlMapper(false).build();
    SimpleModule module = new SimpleModule();

    // Long转String序列化
    module.addSerializer(Long.class, new LongToStringSerializer());
    module.addSerializer(Long.TYPE, new LongToStringSerializer());

    // String转Long反序列化
    module.addDeserializer(Long.class, new StringToLongDeserializer());
    module.addDeserializer(Long.TYPE, new StringToLongDeserializer());
    // 日期序列化与反序列化
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
    module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
    // 时间序列化与反序列化
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
    MultiFormatLocalDateTimeDeserializer localDateTimeDeserializer =
        new MultiFormatLocalDateTimeDeserializer();
    localDateTimeDeserializer.addFormat("yyyy/MM/dd HH:mm:ss");
    localDateTimeDeserializer.addFormat("yyyy/MM/dd HH:mm");
    module.addDeserializer(LocalDateTime.class, localDateTimeDeserializer);

    // 注册自定义模块
    objectMapper.registerModule(module);

    // 注册Hibernate模块来处理懒加载
    Hibernate6Module hibernate6Module = new Hibernate6Module();
    hibernate6Module.disable(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION);
    hibernate6Module.enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
    objectMapper.registerModule(hibernate6Module);

    // 配置序列化选项
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    return objectMapper;
  }

  // 自定义的 LocalDateTime 反序列化器，支持多个格式
  public static class MultiFormatLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private final Map<Integer, DateTimeFormatter> patternLength2formatter = new HashMap<>(6);

    public void addFormat(String pattern) {
      if (StringUtils.isEmpty(pattern)) {
        throw new IllegalArgumentException("pattern can not be empty");
      }
      patternLength2formatter.put(pattern.length(), DateTimeFormatter.ofPattern(pattern));
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      String dateString = p.getText().trim();
      DateTimeFormatter formatter = patternLength2formatter.get(dateString.length());
      if (formatter != null) {
        return LocalDateTime.parse(dateString, formatter);
      }
      throw new IllegalArgumentException("Invalid date format: " + dateString);
    }
  }

  public static class LongToStringSerializer extends JsonSerializer<Long> {
    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      if (value != null) {
        gen.writeString(value.toString());
      }
    }
  }

  public static class StringToLongDeserializer extends JsonDeserializer<Long> {
    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      String value = p.getValueAsString();
      if (value == null || value.trim().isEmpty()) {
        return null;
      }
      try {
        return Long.parseLong(value.trim());
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid Long value: " + value);
      }
    }
  }
}
