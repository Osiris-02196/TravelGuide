package com.oxiris.travelguide.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户状态校验注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StatusCheck {
    /**
     * 允许的用户状态数组，默认只允许正常状态
     */
    String[] allowedStatus() default "1";
}
