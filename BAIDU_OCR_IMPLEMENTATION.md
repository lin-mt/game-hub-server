# 百度OCR集成说明（每月额度版）

## 概述

本项目已集成百度OCR服务用于识别游戏属性图片，支持**5种OCR服务轮询使用**，优先使用百度OCR的免费额度（**每月**），当所有服务的免费额度用完后自动切换到本地Tesseract OCR。

## 核心特性

✅ **5种服务轮询**: 自动轮询使用5种百度OCR服务，最大化利用免费额度  
✅ **智能选择**: 优先使用剩余额度最多的服务  
✅ **数据库管理**: 实时记录每种服务的使用情况（已用/总量/余量）  
✅ **QPS限流**: 每种服务独立限流，确保不超过2次/秒  
✅ **原子更新**: 使用数据库原子操作更新额度，避免并发问题  
✅ **自动降级**: 所有百度OCR服务不可用时自动切换到本地OCR  
✅ **每月重置**: 每月自动初始化新的额度记录  
✅ **动态文件类型**: 支持任意图片格式（PNG、JPG等）

## 重要修复

### 1. 免费额度周期：每月而非每天
- 百度OCR免费额度是**每月**1000次（或500次），不是每天
- 数据库字段从 `quota_date` 改为 `quota_month`（格式：yyyy-MM）
- 每月1号自动初始化新的额度记录

### 2. 并发安全：数据库原子操作
- 使用 `@Modifying` 查询直接在数据库层面更新额度
- 避免了异步更新可能导致的并发问题
- SQL: `UPDATE ... SET used_quota = used_quota + 1, remaining_quota = remaining_quota - 1 WHERE id = ? AND remaining_quota > 0`

### 3. 动态文件扩展名
- 临时文件扩展名根据上传文件类型动态确定
- 支持 PNG、JPG、JPEG、BMP 等各种图片格式  

## 实现细节

### 1. 数据库实体

**文件**: `com.app.gamehub.entity.BaiduOcrQuota`

记录每种服务每月的额度使用情况：
- `service_type`: 服务类型（5种）
- `quota_month`: 额度月份（格式：yyyy-MM）
- `total_quota`: 总额度
- `used_quota`: 已使用额度
- `remaining_quota`: 剩余额度
- `last_request_time`: 最后请求时间（用于QPS限流）

### 2. 核心服务

**文件**: `com.app.gamehub.service.BaiduOcrService`

主要功能：
- **服务轮询**: 自动选择剩余额度最多的服务
- **QPS限流**: 每种服务独立限流（500ms间隔）
- **额度管理**: 使用数据库原子操作更新额度（避免并发问题）
- **自动初始化**: 每月首次使用时自动初始化额度记录
- **原子更新**: 使用 `@Modifying` 查询直接在数据库更新，避免并发冲突

### 3. 5种OCR服务

| 服务类型 | 服务名称 | 每月免费额度 | API路径 |
|---------|---------|------------|---------|
| general_basic | 通用文字识别（标准版） | 1000次 | /rest/2.0/ocr/v1/general_basic |
| general | 通用文字识别（标准含位置版） | 1000次 | /rest/2.0/ocr/v1/general |
| accurate_basic | 通用文字识别（高精度版） | 1000次 | /rest/2.0/ocr/v1/accurate_basic |
| accurate | 通用文字识别（高精度含位置版） | 500次 | /rest/2.0/ocr/v1/accurate |
| webimage | 网络图片文字识别 | 1000次 | /rest/2.0/ocr/v1/webimage |

**总计**: 每月最多 **4500次** 免费识别

### 4. 数据库表结构

```sql
CREATE TABLE baidu_ocr_quota (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_type VARCHAR(50) NOT NULL,
    quota_month VARCHAR(7) NOT NULL,  -- 格式：yyyy-MM
    total_quota INT NOT NULL,
    used_quota INT NOT NULL DEFAULT 0,
    remaining_quota INT NOT NULL,
    last_request_time BIGINT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE KEY uk_service_month (service_type, quota_month)
);
```

### 5. API接口

**查询本月额度统计**:
```
GET /api/baidu-ocr-quota/month
```

返回示例：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "serviceType": "general_basic",
      "quotaMonth": "2026-01",
      "totalQuota": 1000,
      "usedQuota": 150,
      "remainingQuota": 850
    },
    {
      "serviceType": "accurate_basic",
      "quotaMonth": "2026-01",
      "totalQuota": 1000,
      "usedQuota": 200,
      "remainingQuota": 800
    }
    // ... 其他服务
  ]
}
```

## 使用流程

1. **用户上传图片** → 调用 `GameAccountService.updateStatsFromImage()`
   - 支持任意图片格式（PNG、JPG、JPEG、BMP等）
   - 临时文件扩展名根据上传文件动态确定
2. **图片预处理** → `ImageUtils.cropLeftHalf()`
   - 裁剪图片的左半部分
   - 去除右侧干扰信息，提高识别准确率
   - 生成临时裁剪文件
3. **尝试百度OCR** → `BaiduOcrService.parseStats()`
   - 查询本月可用服务（剩余额度 > 0）
   - 按剩余额度降序排列
   - 依次尝试每个服务
4. **服务选择逻辑**:
   - 优先使用剩余额度最多的服务
   - QPS限流：确保单个服务请求间隔 ≥ 500ms
   - 失败自动切换到下一个服务
5. **额度更新**: 识别成功后使用数据库原子操作更新额度（避免并发问题）
6. **清理临时文件**: 自动删除裁剪后的临时文件
7. **降级处理**: 所有服务都不可用时切换到本地OCR

## 性能优化

### 1. 数据库原子操作
使用 `@Modifying` 查询直接在数据库层面更新额度，避免并发冲突：
```java
@Modifying
@Query("UPDATE BaiduOcrQuota q SET q.usedQuota = q.usedQuota + 1, q.remainingQuota = q.remainingQuota - 1, q.updatedAt = CURRENT_TIMESTAMP WHERE q.id = :id AND q.remainingQuota > 0")
int incrementUsedQuota(@Param("id") Long id);
```

### 2. 数据库索引
- `uk_service_month`: 唯一索引，快速查询特定服务的额度
- `idx_quota_month`: 月份索引，快速查询当月所有服务
- `idx_remaining`: 剩余额度索引，快速排序可用服务

### 3. 悲观锁
使用 `@Lock(PESSIMISTIC_WRITE)` 防止并发初始化冲突：
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<BaiduOcrQuota> findByServiceTypeAndQuotaMonthWithLock(...);
```

### 4. 智能轮询
按剩余额度降序选择服务，均衡使用各服务额度。

## 错误处理

### 百度OCR错误码处理
- **错误码 4/17/19**: 额度不足 → 标记该服务不可用，尝试下一个
- **错误码 18**: QPS超限 → 尝试下一个服务
- **其他错误**: 网络异常等 → 尝试下一个服务
- **所有服务都失败**: 切换到本地OCR

### 日志示例

成功使用百度OCR：
```
INFO  - 尝试使用百度OCR识别图片
INFO  - 尝试使用百度OCR服务: general_basic (剩余额度: 850/1000)
INFO  - 百度OCR识别成功，使用服务: general_basic
```

切换服务：
```
INFO  - 尝试使用百度OCR服务: accurate_basic (剩余额度: 0/1000)
WARN  - 服务 accurate_basic 识别失败: 百度OCR免费额度已用完
INFO  - 尝试使用百度OCR服务: general_basic (剩余额度: 850/1000)
INFO  - 百度OCR识别成功，使用服务: general_basic
```

降级到本地OCR：
```
INFO  - 尝试使用百度OCR识别图片
WARN  - 百度OCR识别失败: 所有百度OCR服务的免费额度已用完, 切换到本地OCR
INFO  - 本地OCR识别成功
```

## 配置说明

### API密钥
配置在 `BaiduOcrService` 类中：
```java
private static final String API_KEY = "nzGHDeN4xeCRUZ1bcP9e6IRS";
private static final String SECRET_KEY = "qCdOvBFyEspvK8flT5T0BGLvspD5RQAf";
```

**注意**: 生产环境建议将密钥配置到环境变量或配置文件中。

### 异步配置
在 `AsyncConfig` 中配置线程池：
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        return executor;
    }
}
```

## 监控建议

### 关键指标
1. **每月总调用次数**: 监控是否接近4500次上限
2. **各服务使用分布**: 确保负载均衡
3. **本地OCR使用频率**: 监控百度OCR可用性
4. **识别成功率**: 百度OCR vs 本地OCR
5. **平均响应时间**: 识别性能监控

### 监控查询
```sql
-- 查询本月各服务使用情况
SELECT service_type, total_quota, used_quota, remaining_quota
FROM baidu_ocr_quota
WHERE quota_month = DATE_FORMAT(CURDATE(), '%Y-%m')
ORDER BY remaining_quota DESC;

-- 查询历史使用趋势
SELECT quota_month, SUM(used_quota) as total_used
FROM baidu_ocr_quota
GROUP BY quota_month
ORDER BY quota_month DESC
LIMIT 12;
```

## 优势对比

### 单服务方案 vs 轮询方案

| 特性 | 单服务方案 | 轮询方案（当前） |
|-----|----------|---------------|
| 每月免费额度 | 1000次 | 4500次 |
| 额度用完后 | 立即降级 | 自动切换其他服务 |
| 性能 | 一般 | 更好（原子操作） |
| 可用性 | 较低 | 高（5个服务冗余） |
| 监控 | 简单 | 详细（数据库记录） |
| 并发安全 | 可能有问题 | 安全（原子操作） |

## 未来优化方向

1. ✅ 支持多账号轮询（进一步扩大额度）
2. ✅ 添加告警机制（额度不足时通知）
3. ✅ 优化图片预处理（提高识别准确率）
4. ✅ 添加识别结果缓存（相同图片不重复识别）
5. ✅ 支持手动重置额度（测试环境）

## 依赖项

```xml
<!-- OkHttp for Baidu OCR API -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>

<!-- JSON library -->
<dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20240303</version>
</dependency>
```

## 数据库迁移

运行以下SQL脚本创建表：
```bash
# 位置: src/main/resources/db/migration/V1__create_baidu_ocr_quota_table.sql
```

或手动执行：
```sql
CREATE TABLE IF NOT EXISTS baidu_ocr_quota (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_type VARCHAR(50) NOT NULL,
    quota_month VARCHAR(7) NOT NULL,  -- 格式：yyyy-MM
    total_quota INT NOT NULL,
    used_quota INT NOT NULL DEFAULT 0,
    remaining_quota INT NOT NULL,
    last_request_time BIGINT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE KEY uk_service_month (service_type, quota_month),
    INDEX idx_quota_month (quota_month),
    INDEX idx_remaining (remaining_quota)
);
```

## 重要变更说明

### v2.1 更新（2026-01-16）

**新增图片预处理功能**:
- 在调用百度OCR之前自动裁剪图片的左半部分
- 去除右侧干扰信息，提高识别准确率
- 自动清理裁剪后的临时文件

### v2.0 更新（2026-01-16）

1. **免费额度周期修正**: 从每天改为每月
   - 百度OCR免费额度是每月1000次（或500次），不是每天
   - 数据库字段 `quota_date` → `quota_month`
   - 查询方法 `findByQuotaDate` → `findByQuotaMonth`

2. **并发安全性增强**: 从异步更新改为原子操作
   - 移除 `@Async` 异步更新方法
   - 新增 `@Modifying` 原子更新查询
   - 避免高并发下的额度统计错误

3. **文件类型支持**: 动态识别上传文件扩展名
   - 临时文件扩展名根据上传文件动态确定
   - 支持 PNG、JPG、JPEG、BMP 等各种图片格式
