package com.oxiris.travelguide.model.enums.strategy;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

/**
 * 官方推荐状态枚举
 */
@Getter
public enum OfficialStatusEnum {

    NORMAL(0, "普通"),
    OFFICIAL(1, "官方推荐");

    private final Integer value;
    private final String text;

    OfficialStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static OfficialStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (OfficialStatusEnum anEnum : OfficialStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
