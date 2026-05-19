package com.oxiris.travelguide.model.enums.report;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 举报原因枚举
 */
@Getter
public enum ReportReasonEnum {

    EROTIC("色情低俗"),
    ADVERTISING("广告营销"),
    PERSONAL_ATTACK("人身攻击"),
    ILLEGAL("违法违规"),
    FALSE_INFO("虚假信息"),
    PLAGIARISM("抄袭搬运"),
    OTHER("其他");

    private final String text;

    ReportReasonEnum(String text) {
        this.text = text;
    }

    public static ReportReasonEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (ReportReasonEnum anEnum : ReportReasonEnum.values()) {
            if (anEnum.name().equalsIgnoreCase(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
