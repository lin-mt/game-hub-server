# OCR属性数据错误处理优化 - 实现总结

## 需求
在使用OCR服务识别属性信息时，如果是异常：属性数据错误，确认图片中包含步兵防御力和弓兵生命值属性信息，则直接将该异常抛出，不再尝试其他OCR服务商或本地服务。同时，在抛出异常的信息中去掉服务商名称，只在打印日志时打印服务商名称。

## 实现方案

### 1. 创建新的异常类 - `OcrAttributeException`
- 位置：`src/main/java/com/app/gamehub/exception/OcrAttributeException.java`
- 用途：标记属性数据错误异常，这是一种不可挽回的错误
- 特点：当抛出此异常时，不应该尝试其他OCR服务商或降级到本地服务

### 2. 修改BaiduOcrServiceImpl
- **extractStats方法**：
  - 将抛出的异常从 `BusinessException` 改为 `OcrAttributeException`
  - 异常消息保持不变：`"属性数据错误，确认图片中包含步兵防御力和弓兵生命值属性信息"`

- **parseStats方法**：
  - 添加新的catch块处理 `OcrAttributeException`
  - 捕获到此异常时，记录日志并直接抛出，不继续尝试其他服务
  - 日志中仍包含服务商名称：`log.error("百度OCR服务 {} 属性数据错误，直接抛出异常", quota.getServiceType(), oae)`

- **性能相关异常消息调整**：
  - 移除服务商名称前缀，改为通用格式：
    - `"百度OCR QPS超限"` → `"OCR服务QPS超限"`
    - `"百度OCR免费额度已用完"` → `"OCR服务免费额度已用完"`
    - `"百度OCR识别失败"` → `"OCR识别失败"`
  - 移除服务商名称前缀：
    - `"百度OCR服务的免费额度已用完"` → `"OCR服务的免费额度已用完"`
    - `"百度OCR所有服务都不可用"` → `"OCR所有服务都不可用"`

### 3. 修改TencentOcrServiceImpl
- **extractStats方法**：
  - 将抛出的异常从 `BusinessException` 改为 `OcrAttributeException`
  - 异常消息保持不变：`"属性数据错误，确认图片中包含步兵防御力和弓兵生命值属性信息"`

- **parseStats方法**：
  - 添加新的catch块处理 `OcrAttributeException`
  - 捕获到此异常时，记录日志并直接抛出，不继续尝试其他服务
  - 日志中仍包含服务商名称：`log.error("腾讯云OCR服务 {} 属性数据错误，直接抛出异常", quota.getServiceType(), oae)`

- **性能相关异常消息调整**：
  - 移除服务商名称前缀，改为通用格式：
    - `"腾讯云OCR服务 * 额度已用完"` → `"OCR服务额度已用完"`
    - `"腾讯云OCR服务 * QPS限制"` → `"OCR服务QPS限制"`
    - `"腾讯云OCR识别失败"` → `"OCR识别失败"`
  - 移除密钥配置异常中的服务商名称：
    - `"腾讯云OCR密钥未配置"` → `"OCR密钥未配置"`
  - 移除服务商名称前缀：
    - `"腾讯云OCR服务的免费额度已用完"` → `"OCR服务的免费额度已用完"`
    - `"腾讯云OCR所有服务都不可用"` → `"OCR所有服务都不可用"`

### 4. 修改OcrServiceManager
- **parseStats方法**：
  - 添加新的catch块处理 `OcrAttributeException`
  - 捕获到此异常时，记录日志（包含服务商名称）并直接抛出：
    ```java
    log.error("OCR服务商 {} 属性数据错误，直接抛出异常", service.getProviderName(), oae);
    throw oae;
    ```
  - 此处理确保属性数据错误异常不会被降级处理，直接传递给调用者

## 异常流程变化

### 原流程（修改前）
```
属性数据错误异常
  ↓
捕获为BusinessException
  ↓
继续尝试下一个OCR服务商
  ↓
尝试本地OCR
  ↓
最终抛出通用异常
```

### 新流程（修改后）
```
属性数据错误异常
  ↓
抛出OcrAttributeException
  ↓
BaiduOcrServiceImpl/TencentOcrServiceImpl catch并日志记录（包含服务名称）
  ↓
直接抛出，不尝试其他服务
  ↓
OcrServiceManager catch并日志记录（包含服务商名称）
  ↓
直接抛出异常消息（不含服务商名称）
  ↓
调用者接收到不含服务商名称的异常
```

## 日志打印示例

### 属性数据错误时的日志
```
ERROR: 百度OCR服务 general_basic 属性数据错误，直接抛出异常
ERROR: OCR服务商 baidu 属性数据错误，直接抛出异常
```

### 异常消息（不含服务商名称）
```
属性数据错误，确认图片中包含步兵防御力和弓兵生命值属性信息
```

## 关键特性
1. ✅ 属性数据错误时直接抛出，不再尝试其他服务
2. ✅ 异常消息中不含服务商名称
3. ✅ 日志记录仍包含完整的服务商信息（便于调试）
4. ✅ 保持向后兼容性，其他类型错误的处理逻辑不变
5. ✅ 代码无编译错误，完整可用

## 文件修改列表
- ✅ `src/main/java/com/app/gamehub/exception/OcrAttributeException.java` （新建）
- ✅ `src/main/java/com/app/gamehub/service/ocr/BaiduOcrServiceImpl.java`
- ✅ `src/main/java/com/app/gamehub/service/ocr/TencentOcrServiceImpl.java`
- ✅ `src/main/java/com/app/gamehub/service/ocr/OcrServiceManager.java`
