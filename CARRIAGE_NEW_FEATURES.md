# 联盟马车新功能说明

## 新增功能

### 1. 设置今日车主
**功能描述**：允许手动选择联盟成员设置为今日车主

**特殊逻辑**：
- 如果今日已有车主，原车主会被自动插入到队伍第一名
- 原车主的 `lastDriveDate` 会被清空
- 新车主的 `lastDriveDate` 会被设置为今天

**API接口**：
- 端点：`POST /api/carriage/set-driver`
- 请求体：
```json
{
  "allianceId": 1,
  "accountId": 2
}
```

**业务流程**：
1. 验证联盟和账号存在性
2. 验证账号属于该联盟
3. 检查账号是否在排队列表中
4. 查找今日是否已有车主
5. 如果有车主：
   - 清空原车主的 lastDriveDate
   - 将所有排队顺序>=1的记录+1
   - 设置原车主排队顺序为1
6. 设置新车主的 lastDriveDate 为今天

---

### 2. 从队伍中移除成员
**功能描述**：允许从马车队伍中移除指定成员

**API接口**：
- 端点：`POST /api/carriage/remove`
- 请求体：
```json
{
  "allianceId": 1,
  "accountId": 2
}
```

**业务流程**：
1. 验证联盟存在
2. 查找排队记录
3. 删除排队记录
4. 更新后续排队顺序（所有排队号>当前号的记录-1）

---

### 3. 在队伍中插队（添加成员）
**功能描述**：允许在马车队伍的指定位置插入成员

**API接口**：
- 端点：`POST /api/carriage/insert`
- 请求体：
```json
{
  "allianceId": 1,
  "accountId": 2,
  "position": 3
}
```

**业务流程**：
1. 验证联盟和账号存在性
2. 验证账号属于该联盟
3. 检查账号是否已在排队列表中
4. 验证插入位置（必须>=1）
5. 如果插入位置超过最大值，则插入到末尾
6. 将插入位置及之后的记录排队顺序+1
7. 创建新的排队记录，排队顺序为指定位置

---

## 数据库变更

### CarriageQueueRepository 新增方法
```java
/**
 * 将指定联盟中排队顺序大于等于指定值的记录的顺序号加1
 */
@Modifying
@Query("UPDATE CarriageQueue cq SET cq.queueOrder = cq.queueOrder + 1 WHERE cq.allianceId = :allianceId AND cq.queueOrder >= :queueOrder")
void incrementQueueOrderFrom(@Param("allianceId") Long allianceId, @Param("queueOrder") Integer queueOrder);
```

---

## 前端变更

### API 新增方法（src/services/api.ts）
```typescript
// 设置今日车主
setDriver: (data: {
  allianceId: string;
  accountId: string;
}) => Promise<ApiResponse<void>>

// 从队伍中移除成员
removeMember: (data: {
  allianceId: string;
  accountId: string;
}) => Promise<ApiResponse<void>>

// 在队伍中插队添加成员
insertMember: (data: {
  allianceId: string;
  accountId: string;
  position: number;
}) => Promise<ApiResponse<void>>
```

### 页面新增功能（carriage.tsx）
1. **成员管理按钮**：每个队伍成员旁边显示"设为车主"和"移除"按钮
2. **插队添加按钮**：在队伍列表顶部显示"插队添加成员"按钮
3. **对话框**：
   - 设置车主确认对话框
   - 移除成员确认对话框
   - 插队添加成员对话框（包含成员选择和位置输入）

---

## 使用场景

### 场景1：手动指定今日车主
当自动分配的车主不合适时，盟主可以手动选择其他成员作为今日车主。原车主会自动排到队伍第一位，明天可以继续开车。

### 场景2：移除不活跃成员
当某个成员长期不活跃或退出联盟时，可以将其从马车队伍中移除。

### 场景3：优先安排重要成员
当有新成员加入或需要优先安排某个成员时，可以使用插队功能将其插入到指定位置。

---

## 注意事项

1. **权限控制**：目前所有登录用户都可以使用这些功能，建议后续添加盟主权限验证
2. **并发控制**：使用了 `@Transactional` 注解确保数据一致性
3. **排队顺序维护**：所有操作都会自动维护排队顺序的连续性
4. **今日车主标记**：通过 `lastDriveDate` 字段标记，定时任务会在每天自动清理和分配
