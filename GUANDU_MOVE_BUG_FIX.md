# 官渡战事移动BUG修复

## 问题描述

在官渡一和官渡二中，当用户将已经报名官渡一的账号A移动到官渡二，将已经报名官渡二的账号B移动到官渡一，然后应用战术之后，账号A又回到了官渡一中，账号B又回到了官渡二中。

## 根本原因

问题的根本原因是 `moveGuanDuWar` 方法只更新了 `WarArrangement` 的 `warType`，但没有同步更新对应的 `WarApplication` 的 `warType`。这导致数据不一致：

1. **移动后的状态**：
   - 账号A的 `WarArrangement.warType` = `GUANDU_TWO`
   - 账号A的 `WarApplication.warType` = `GUANDU_ONE`（未更新）

2. **应用战术时的问题**：
   - 在官渡二应用战术时，系统查找官渡二的申请者，但找不到账号A的官渡二申请记录
   - 在官渡一应用战术时，系统找到账号A的官渡一申请记录，重新创建官渡一的 `WarArrangement`
   - 结果：账号A又回到了官渡一

## 解决方案

修改 `moveGuanDuWar` 方法，同时更新 `WarArrangement` 和 `WarApplication` 的战事类型：

```java
public WarArrangement moveGuanDuWar(Long accountId) {
    // ... 验证逻辑 ...
    
    WarType newWarType;
    if (warArrangement.getWarType().equals(WarType.GUANDU_ONE)) {
        newWarType = WarType.GUANDU_TWO;
    } else {
        newWarType = WarType.GUANDU_ONE;
    }
    
    // 更新战事安排的类型
    warArrangement.setWarType(newWarType);
    warArrangement.setWarGroupId(null); // 清除分组信息，等待重新分配
    
    // 同时更新对应的战事申请的类型
    List<WarApplication> applications = warApplicationRepository.findByAccountIdAndWarTypeIn(
        accountId, List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO));
    for (WarApplication application : applications) {
        if (application.getStatus() == WarApplication.ApplicationStatus.APPROVED) {
            application.setWarType(newWarType);
            warApplicationRepository.save(application);
        }
    }
    
    warArrangementRepository.save(warArrangement);
    return warArrangement;
}
```

### 关键改进

1. **数据一致性**：同时更新 `WarArrangement` 和 `WarApplication` 的 `warType`
2. **状态同步**：确保申请记录和安排记录的战事类型保持一致
3. **简单清理**：战术应用时只需要清理当前战事类型的数据，保持官渡一和官渡二的独立性

## 修复效果

修复后的完整流程：

1. **移动账号**：
   - 账号A从官渡一移动到官渡二
     - `WarArrangement.warType` → `GUANDU_TWO`
     - `WarApplication.warType` → `GUANDU_TWO`
   - 账号B从官渡二移动到官渡一
     - `WarArrangement.warType` → `GUANDU_ONE`
     - `WarApplication.warType` → `GUANDU_ONE`

2. **应用战术**：
   - 在官渡一应用战术：只清理官渡一的数据，重新分配官渡一的申请者（包括账号B）
   - 在官渡二应用战术：只清理官渡二的数据，重新分配官渡二的申请者（包括账号A）

3. **结果**：
   - 账号A留在官渡二，获得新的战术分组
   - 账号B留在官渡一，获得新的战术分组
   - 两个官渡战事保持独立，互不影响

## 新增Repository方法

为了支持这个修复，添加了必要的Repository方法：

### WarApplicationRepository
```java
List<WarApplication> findByAccountIdAndWarTypeIn(Long accountId, Collection<WarType> warTypes);
```

### WarArrangementRepository
```java
List<WarArrangement> findByAllianceIdAndWarTypeIn(Long allianceId, Collection<WarType> warTypes);
```

## 测试覆盖

### 移动功能测试 (`WarServiceMoveGuanDuTest`)
- ✅ 从官渡一移动到官渡二，同时更新安排和申请记录
- ✅ 从官渡二移动到官渡一，同时更新安排和申请记录
- ✅ 没有分组的账号移动，正确处理
- ✅ 各种异常情况的处理

### 战术应用测试 (`WarServiceGuanduCleanupTest`)
- ✅ 应用官渡一战术时，只清理官渡一数据
- ✅ 应用官渡二战术时，只清理官渡二数据

### 现有功能测试
- ✅ 所有现有的战术和战事相关测试继续通过

## 影响范围

- **修改文件**：
  - `WarService.java` - 修改 `moveGuanDuWar` 方法
  - `WarApplicationRepository.java` - 添加 `findByAccountIdAndWarTypeIn` 方法
  - `WarArrangementRepository.java` - 添加 `findByAllianceIdAndWarTypeIn` 方法
- **测试文件**：
  - 更新 `WarServiceMoveGuanDuTest.java` - 验证申请记录同步更新
  - 更新 `WarServiceGuanduCleanupTest.java` - 验证独立清理逻辑
- **向后兼容**：完全兼容现有功能
- **性能影响**：微小（移动时增加一次申请记录的查询和更新）

## 验证方法

1. **运行所有战事相关测试**：`./mvnw test -Dtest="*War*Test"`
2. **手动测试流程**：
   - 创建官渡战事并应用战术
   - 移动部分账号到另一个官渡战事
   - 分别在两个官渡战事中应用战术
   - 验证账号位置保持移动后的状态，不会回到原来的位置

## 设计优势

1. **数据一致性**：确保 `WarArrangement` 和 `WarApplication` 的战事类型始终保持同步
2. **独立性**：官渡一和官渡二保持完全独立，互不影响
3. **简单可靠**：逻辑清晰，易于理解和维护
4. **最小影响**：只修改移动逻辑，战术应用逻辑保持原有的简洁性