package com.oxiris.travelguide.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import com.mybatisflex.core.keygen.KeyGenerators;

import lombok.*;

/**
* 用户 实体类。
 *
 * @author <a href="https://github.com/Osiris-02196">cpy</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 用户名
     */
    @Column("userAccount")
    private String userAccount;

    /**
     *姓名
     */
    @Column("userName")
    private String userName;

    /**
     * 密码
     */
    @Column("userPassword")
    private String userPassword;

    /**
     * 头像
     */
    @Column("userAvatar")
    private String userAvatar;

    /**
     * 角色（0-超级管理员 1-管理员 2-普通用户）
     */
    @Column("userRole")
    private String userRole;

/**
     * 状态（1-正常 2-禁言 3-封号）
     */
    @Column("userStatus")
    private Integer userStatus;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

}
