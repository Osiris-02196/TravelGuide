package com.oxiris.travelguide.model.enums.user;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户状态枚举值
 */
@Getter
public enum UserStatusEnum {

    NORMAL(1, "正常"),
    MUTED(2, "禁言"),
    BANNED(3, "封号");

    private final Integer value;
    private final String text;

    UserStatusEnum(Integer value, String text){
        this.value = value;
        this.text = text;
    }
    /**
     * 获取值列表
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(UserStatusEnum::getValue).collect(Collectors.toList());
    }
    /**
     * 根据 value 获取枚举
     */
    public static UserStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (UserStatusEnum anEnum : UserStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
