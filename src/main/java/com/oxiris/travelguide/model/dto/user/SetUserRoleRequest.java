package com.oxiris.travelguide.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 设置用户角色请求
 */
@Data
public class SetUserRoleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户角色：user/admin
     */
    private String userRole;
}
