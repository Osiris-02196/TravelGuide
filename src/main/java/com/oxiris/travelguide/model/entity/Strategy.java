package com.oxiris.travelguide.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 攻略 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("strategy")
public class Strategy implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 攻略ID
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 发布用户ID
     */
    @Column("userId")
    private Long userId;

    /**
     * 攻略标题
     */
    @Column("strategyTitle")
    private String strategyTitle;

    /**
     * 攻略内容
     */
    @Column("strategyContent")
    private String strategyContent;

    /**
     * 攻略图片(JSON格式)
     */
    @Column("imageUrls")
    private String imageUrls;

    /**
     * 攻略标签（逗号分隔）
     */
    @Column("strategyTags")
    private String strategyTags;

    /**
     * 审核状态（0-待审核，1-通过，2-拒绝）
     */
    @Column("strategyStatus")
    private Integer strategyStatus;

    /**
     * 点击量
     */
    @Column("clickCount")
    private Integer clickCount;

    /**
     * 点赞量
     */
    @Column("likeCount")
    private Integer likeCount;

    /**
     * 收藏量
     */
    @Column("collectCount")
    private Integer collectCount;

    /**
     * 评论数
     */
    @Column("commentCount")
    private Integer commentCount;

    /**
     * 热度分数
     */
    @Column("hotScore")
    private Double hotScore;

    /**
     * 地点标签（JSON数组格式，如["北京","上海"]）
     */
    @Column("locations")
    private String locations;

    /**
    * 路线规划数据（JSON格式）
    */
    @Column("routeData")
    private String routeData;

    /**
     * 是否为官方推荐（0-否 1-是）
     */
    @Column("isOfficial")
    private Integer isOfficial;

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
