package com.oxiris.travelguide.model.enums.notify;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
public enum NotifyTypeEnum {

    SYSTEM("system", "系统通知"),
    LIKE("like", "点赞通知"),
    COMMENT("comment", "评论通知"),
    COLLECT("collect", "收藏通知"),
    PENDING("pending", "待审核通知"),
    VIOLATION("violation", "违规通知"),
    REPORT_RESULT("report_result", "举报结果通知");

    private final String value;
    private final String text;

    NotifyTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static NotifyTypeEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (NotifyTypeEnum anEnum : NotifyTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
