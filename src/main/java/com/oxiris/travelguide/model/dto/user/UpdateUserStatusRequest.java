package com.oxiris.travelguide.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新用户状态请求
 */
@Data
public class UpdateUserStatusRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户状态（1正常/2禁言/3封号）
     */
    private Integer userStatus;
}