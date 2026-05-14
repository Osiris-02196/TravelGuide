package com.oxiris.travelguide.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 攻略收藏视图（包含收藏记录和攻略摘要信息）
 */
@Data
public class StrategyCollectVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 收藏记录ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 攻略ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long strategyId;

    /**
     * 收藏时间
     */
    private LocalDateTime createTime;

    // ===== 攻略摘要信息 =====
    /**
     * 攻略标题
     */
    private String strategyTitle;

    /**
     * 攻略封面图片（取第一张）
     */
    private String coverImage;

    /**
     * 攻略全部图片（JSON数组字符串）
     */
    private String imageUrls;

    /**
     * 攻略内容摘要
     */
    private String strategyContent;

    /**
     * 攻略地点（JSON数组字符串）
     */
    private String locations;

    /**
     * 攻略标签
     */
    private String strategyTags;

    /**
     * 发布用户名称
     */
    private String userName;

    /**
     * 发布用户头像
     */
    private String userAvatar;

    /**
     * 收藏量
     */
    private Integer collectCount;

    /**
     * 点赞量
     */
    private Integer likeCount;
}