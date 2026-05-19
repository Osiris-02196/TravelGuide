package com.oxiris.travelguide.model.enums.strategy;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

/**
 * 攻略状态枚举
 */
@Getter
public enum StrategyStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "通过"),
    REJECTED(2, "拒绝");

    private final Integer value;
    private final String text;

    StrategyStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static StrategyStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (StrategyStatusEnum anEnum : StrategyStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
