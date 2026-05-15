package com.oxiris.travelguide.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 举报目标类型枚举
 */
@Getter
public enum ReportTargetTypeEnum {

    STRATEGY("strategy", "攻略"),
    COMMENT("comment", "评论");

    private final String value;
    private final String text;

    ReportTargetTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static ReportTargetTypeEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (ReportTargetTypeEnum anEnum : ReportTargetTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
