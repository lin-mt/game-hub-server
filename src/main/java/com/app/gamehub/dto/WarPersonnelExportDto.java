package com.app.gamehub.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WarPersonnelExportDto {
    @ExcelProperty("分组")
    private String groupName;

    @ExcelProperty("是否替补")
    private String isSubstitute;

    @ExcelProperty("账号名称")
    private String accountName;

    @ExcelProperty("注册状态")
    private String registrationStatus;

    @ExcelProperty("阶位")
    private String memberTier;

    @ExcelProperty("战力（万）")
    private Long powerValue;

    @ExcelProperty("兵等级")
    private Integer troopLevel;

    @ExcelProperty("吕布星级")
    private BigDecimal lvbuStarLevel;

    @ExcelProperty("最高加成")
    private BigDecimal damageBonus;

    @ExcelProperty("集结容量")
    private Integer rallyCapacity;

    @ExcelProperty("兵数量")
    private Long troopQuantity;

    @ExcelProperty("步兵防御力")
    private Integer infantryDefense;

    @ExcelProperty("步兵生命值")
    private Integer infantryHp;

    @ExcelProperty("弓兵攻击力")
    private Integer archerAttack;

    @ExcelProperty("弓兵破坏力")
    private Integer archerSiege;
}
