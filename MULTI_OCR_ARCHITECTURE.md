# 多服务商OCR架构设计

## 概述

本项目已重构为支持多个OCR服务商的可扩展架构，目前支持百度OCR和腾讯云OCR，后续可轻松添加其他服务商（如阿里云、华为云等）。

## 关键修复

### OCR服务可用性检查修复 ✅
**问题**: `isAvailable()` 方法在首次使用时无法找到额度记录，导致服务永远不可用
**解决**: 在 `isAvailable()` 方法中添加 `initMonthQuotas()` 调用，确保首次检查时自动初始化额度记录

**影响文件**:
- `BaiduOcrServiceImpl.java` - 已修复
- `TencentOcrServiceImpl.java` - 已修复 + 优先级修正（0→2）

**修复后流程**:
```
isAvailable() → initMonthQuotas() → 查询额度记录 → 返回可用性
```

## 架构特性

✅ **多服务商支持**: 统一接口支持多个OCR服务商  
✅ **优先级管理**: 按优先级自动选择最优服务商  
✅ **统一额度管理**: 所有服务商共用一套额度管理系统  
✅ **自动降级**: 云端服务不可用时自动降级到本地OCR  
✅ **易于扩展**: 新增服务商只需实现OcrService接口  
✅ **配置灵活**: 支持环境变量配置密钥  
✅ **监控完善**: 统一的额度监控和服务状态查询

## 架构组件

### 1. 核心接口

**OcrService** - OCR服务抽象接口
```java
public interface OcrService {
    Map<String, Integer> parseStats(File imageFile) throws Exception;
    String getProviderName();
    boolean isAvailable();
    int getPriority();
}
```

### 2. 服务实现

| 服务商 | 实现类 | 优先级 | 状态 |
|--------|--------|--------|------|
| 百度OCR | BaiduOcrServiceImpl | 1 | ✅ 已实现 |
| 腾讯云OCR | TencentOcrServiceImpl | 2 | ✅ 已实现 |
| 阿里云OCR | AliyunOcrServiceImpl | 3 | ⏳ 待实现 |
| 华为云OCR | HuaweiOcrServiceImpl | 4 | ⏳ 待实现 |

### 3. 统一管理

**OcrServiceManager** - OCR服务管理器
- 自动发现所有OCR服务实现
- 使用轮询策略均匀分配负载
- 统一的错误处理和降级策略

**轮询策略优势**：
- ✅ 均匀分配负载到所有可用服务商
- ✅ 充分利用每个服务商的免费额度
- ✅ 避免单一服务商过度使用
- ✅ 提高整体系统可用性

### 4. 数据模型

**OcrQuota** - 通用OCR额度实体
```sql
CREATE TABLE ocr_quota (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,        -- 服务商
    service_type VARCHAR(50) NOT NULL,    -- 服务类型
    quota_month VARCHAR(7) NOT NULL,      -- 额度月份
    total_quota INT NOT NULL,             -- 总额度
    used_quota INT NOT NULL DEFAULT 0,    -- 已使用额度
    remaining_quota INT NOT NULL,         -- 剩余额度
    last_request_time BIGINT DEFAULT 0,   -- 最后请求时间
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE KEY uk_provider_service_month (provider, service_type, quota_month)
);
```

## 服务商配置

### 百度OCR
```java
// 硬编码配置（开发环境）
private static final String API_KEY = "nzGHDeN4xeCRUZ1bcP9e6IRS";
private static final String SECRET_KEY = "qCdOvBFyEspvK8flT5T0BGLvspD5RQAf";

// 支持的服务类型
- general_basic: 通用文字识别（标准版）- 1000次/月
- general: 通用文字识别（标准含位置版）- 1000次/月  
- accurate_basic: 通用文字识别（高精度版）- 1000次/月
- accurate: 通用文字识别（高精度含位置版）- 500次/月
- webimage: 网络图片文字识别 - 1000次/月
```

### 腾讯云OCR
```java
// 环境变量配置（推荐）
private static final String SECRET_ID = System.getenv("TENCENTCLOUD_SECRET_ID");
private static final String SECRET_KEY = System.getenv("TENCENTCLOUD_SECRET_KEY");

// 支持的服务类型
- general_basic: 通用印刷体识别 - 1000次/月
- general_accurate: 通用文字识别（高精度版）- 1000次/月
- table_ocr: 表格识别（V3）- 1000次/月
- advertise_ocr: 广告文字识别 - 1000次/月
```

## API接口

### 1. 查询所有服务商额度
```
GET /api/ocr-quota/month
```

### 2. 查询指定服务商额度
```
GET /api/ocr-quota/month/{provider}
```

### 3. 查询轮询统计
```
GET /api/ocr-quota/round-robin
```

响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCalls": 156,
    "availableServices": 2,
    "currentPosition": 0,
    "nextService": "baidu"
  }
}
```

### 4. 查询服务商状态
```
GET /api/ocr-quota/services
```

响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "providerName": "baidu",
      "available": true,
      "priority": 1
    },
    {
      "providerName": "tencent", 
      "available": true,
      "priority": 2
    }
  ]
}
```

**注意**: 虽然保留了priority字段用于兼容性，但实际使用轮询策略，不再按优先级选择。

## 工作流程

1. **用户上传图片** → `GameAccountService.updateStatsFromImage()`
2. **图片预处理** → `ImageUtils.cropLeftHalf()` 裁剪左半部分
3. **服务选择** → `OcrServiceManager.parseStats()`
   - 获取所有可用服务列表
   - 使用轮询算法选择下一个服务商
   - 依次尝试直到成功或全部失败
4. **OCR识别** → 各服务商实现类
   - 检查额度和QPS限制
   - 调用对应的云端API
   - 解析识别结果
5. **额度更新** → 数据库原子操作更新使用量
6. **降级处理** → 所有云端服务失败时使用本地OCR
7. **返回结果** → 统一的属性Map格式

### 轮询策略详解

```
第1次调用: 百度OCR (位置0)
第2次调用: 腾讯云OCR (位置1)  
第3次调用: 百度OCR (位置0)
第4次调用: 腾讯云OCR (位置1)
...
```

**优势**:
- 负载均匀分配
- 充分利用免费额度
- 避免单点过载
- 提高整体可用性

## 扩展指南

### 添加新的OCR服务商

1. **实现OcrService接口**
```java
@Service("newProviderOcrService")
public class NewProviderOcrServiceImpl implements OcrService {
    
    @Override
    public Map<String, Integer> parseStats(File imageFile) throws Exception {
        // 实现OCR识别逻辑
    }
    
    @Override
    public String getProviderName() {
        return "newprovider";
    }
    
    @Override
    public boolean isAvailable() {
        // 检查服务可用性
    }
    
    @Override
    public int getPriority() {
        return 5; // 设置优先级
    }
}
```

2. **定义服务类型枚举**
```java
public enum NewProviderOcrServiceType {
    SERVICE_TYPE_1("type1", "服务类型1", 1000),
    SERVICE_TYPE_2("type2", "服务类型2", 500);
    
    // 构造函数和getter方法
}
```

3. **实现额度管理**
- 使用统一的OcrQuotaRepository
- 实现initMonthQuotas()方法
- 实现updateQuotaAtomic()方法

4. **Spring自动发现**
- 添加@Service注解
- OcrServiceManager会自动发现并注册

### 配置优先级

优先级数字越小，优先级越高：
- 1: 百度OCR（已实现，稳定）
- 2: 腾讯云OCR（框架已搭建）
- 3-10: 其他服务商

## 监控和运维

### 关键指标
1. **各服务商可用性**: 通过`/api/ocr-quota/services`监控
2. **额度使用情况**: 通过`/api/ocr-quota/month`监控
3. **识别成功率**: 日志中记录各服务商成功/失败次数
4. **响应时间**: 各服务商API调用耗时

### 告警建议
1. 当某服务商额度使用超过80%时告警
2. 当所有服务商都不可用时告警
3. 当识别失败率超过10%时告警

## 优势对比

### 单服务商 vs 多服务商架构

| 特性 | 单服务商（旧） | 多服务商（新） |
|-----|-------------|--------------|
| 可用性 | 低（单点故障） | 高（多重冗余） |
| 额度利用 | 4500次/月 | 8500+次/月 |
| 扩展性 | 差 | 优秀 |
| 维护成本 | 低 | 中等 |
| 故障恢复 | 手动 | 自动 |

### 总额度对比

| 服务商 | 月免费额度 | 备注 |
|--------|-----------|------|
| 百度OCR | 4500次 | 5个服务类型 |
| 腾讯云OCR | 4000次 | 4个服务类型 |
| **总计** | **8500次** | 比单一服务商提升89% |

## 未来规划

1. ~~**完善腾讯云OCR实现** - 集成腾讯云SDK~~ ✅ **已完成**
2. **添加阿里云OCR** - 进一步提升可用性
3. **智能路由** - 根据图片类型选择最优服务商
4. **缓存机制** - 相同图片避免重复识别
5. **性能优化** - 并行调用多个服务商
6. **成本优化** - 根据成本和准确率动态选择服务商

## 配置示例

### 环境变量配置
```bash
# 腾讯云OCR
export TENCENTCLOUD_SECRET_ID="your_secret_id"
export TENCENTCLOUD_SECRET_KEY="your_secret_key"

# 阿里云OCR（未来）
export ALIBABA_CLOUD_ACCESS_KEY_ID="your_access_key"
export ALIBABA_CLOUD_ACCESS_KEY_SECRET="your_access_secret"
```

### 应用配置
```yaml
# application.yml
ocr:
  providers:
    baidu:
      enabled: true
      priority: 1
    tencent:
      enabled: true
      priority: 2
    aliyun:
      enabled: false
      priority: 3
```

这个架构设计确保了系统的高可用性、可扩展性和易维护性，为未来添加更多OCR服务商奠定了坚实的基础。