# 账号合并功能

## 功能描述

当用户修改账号信息时，系统会自动检查联盟中是否存在与新账号名称相同的无主账号。如果存在，系统会自动将无主账号的信息合并到用户账号中，并删除无主账号及其相关联的信息。

## 触发条件

1. 用户修改账号名称
2. 用户账号属于某个联盟
3. 联盟中存在与新账号名称相同的无主账号（userId为null的账号）

## 合并逻辑

### 1. 字段合并
系统会将无主账号的所有非空字段复制到用户账号中，包括：
- 战力值 (powerValue)
- 伤害加成 (damageBonus)
- 部队等级 (troopLevel)
- 集结容量 (rallyCapacity)
- 部队数量 (troopQuantity)
- 步兵防御力 (infantryDefense)
- 步兵生命值 (infantryHp)
- 弓兵攻击力 (archerAttack)
- 弓兵破坏力 (archerSiege)
- 吕布星级 (lvbuStarLevel)
- 成员等级 (memberTier)
- 南蛮分组ID (barbarianGroupId)

### 2. 关联记录转移
系统会将无主账号的所有关联记录转移到用户账号：
- 联盟申请记录 (AllianceApplication)
- 战事申请记录 (WarApplication)
- 战事安排记录 (WarArrangement)
- 官职预约记录 (PositionReservation)
- 马车排队记录 (CarriageQueue)

### 3. 清理操作
- 删除无主账号
- 清除EntityManager缓存确保数据一致性

## 实现位置

- **主要实现**: `GameAccountService.updateGameAccount()`
- **合并逻辑**: `GameAccountService.mergeUnownedAccountToUserAccount()`
- **数据库操作**: 各Repository的`transferToAccount()`方法

## 测试覆盖

- `GameAccountServiceAccountMergeTest`: 测试账号更新时的合并功能
- `AllianceMemberServiceAccountMergeTest`: 测试加入联盟时的合并功能
- `AllianceMemberServiceUnownedAccountTest`: 测试无主账号相关功能

## 日志记录

系统会记录详细的合并过程日志：
- 发现同名无主账号
- 开始合并过程
- 各类记录转移完成
- 无主账号删除完成
- 合并完成确认

## 注意事项

1. 只有无主账号（userId为null）才会被合并
2. 只有非空字段才会被复制，用户原有数据不会被覆盖
3. 合并过程是事务性的，确保数据一致性
4. 合并完成后会清除缓存，确保后续查询获取最新数据