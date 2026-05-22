package com.oxiris.travelguide.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 攻略信息视图（用于列表和详情展示）
 */
@Data
public class StrategyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 攻略ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 发布用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 发布用户名称
     */
    private String userName;

    /**
     * 发布用户头像
     */
    private String userAvatar;

    /**
     * 攻略标题
     */
    private String strategyTitle;

    /**
     * 攻略内容
     */
    private String strategyContent;

    /**
     * 攻略图片URL列表
     */
    private String imageUrls;
    /**
     * 攻略标签
     */
    private String strategyTags;
    /**
     * 审核状态（0-待审核，1-通过，2-拒绝）
     */
    private Integer strategyStatus;

    /**
     * 点击量
     */
    private Integer clickCount;

    /**
     * 点赞量
     */
    private Integer likeCount;

    /**
     * 收藏量
     */
    private Integer collectCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 热度分数
     */
    private Double hotScore;
    /**
     * 地点标签
     */
    private String locations;
    
    /**
     * 路线规划数据（JSON格式）
     */
    private String routeData;

    /**
     * 是否为官方推荐（0-否 1-是）
     */
    private Integer isOfficial;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}