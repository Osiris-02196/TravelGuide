package com.oxiris.travelguide.model.enums.report;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 举报状态枚举
 */
@Getter
public enum ReportStatusEnum {

    PENDING("pending", "待审核"),
    APPROVED("approved", "举报成立"),
    REJECTED("rejected", "举报驳回");

    private final String value;
    private final String text;

    ReportStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static ReportStatusEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (ReportStatusEnum anEnum : ReportStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
