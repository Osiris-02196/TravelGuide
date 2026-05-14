package com.oxiris.travelguide.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 获取脱敏后的用户信息（用于查询）
 */
@Data
public class UserVO implements Serializable {

    /**
     * id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户状态
     */
    private Integer userStatus;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 关注数
     */
    private Long followCount;

    /**
     * 粉丝数
     */
    private Long followerCount;

    /**
     * 当前用户是否已关注（仅登录后有效）
     */
    private Boolean isFollowed;

    private static final long serialVersionUID = 1L;
}
