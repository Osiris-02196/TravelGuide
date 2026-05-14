package com.oxiris.travelguide.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户修改个人资料请求（仅限修改自己的昵称和头像）
 */
@Data
public class UserUpdateProfileRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像URL
     */
    private String userAvatar;

    private static final long serialVersionUID = 1L;
}