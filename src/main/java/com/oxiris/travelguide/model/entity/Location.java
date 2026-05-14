package com.oxiris.travelguide.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 地点 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("location")
public class Location implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 地点ID（自增主键）
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 地点编码
     */
    @Column("locationCode")
    private String locationCode;

    /**
     * 地点名称
     */
    @Column("locationName")
    private String locationName;

    /**
     * 父级地点ID（0表示顶级）
     */
    @Column("parentId")
    private Long parentId;

    /**
     * 层级（1-国家 2-省/直辖市 3-地级市）
     */
    @Column("level")
    private Integer level;

    /**
     * 点赞量
     */
    @Column("likeCount")
    private Integer likeCount;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除（0-否 1-是）
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}