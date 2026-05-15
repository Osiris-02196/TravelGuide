package com.oxiris.travelguide.constant;

/**
 * 用户常量
 *
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 超级管理员角色
     */
    String SUPERADMIN_ROLE = "superadmin";
    /**
     * 被封号
     */
    String BANNED = "3";
    /**
     * 被禁言
     */
    String MUTED = "2";
    /**
     * 正常
     */
    String NORMAL = "1";
    // endregion
}
