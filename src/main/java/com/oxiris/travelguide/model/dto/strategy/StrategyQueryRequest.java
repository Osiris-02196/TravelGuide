package com.oxiris.travelguide.model.dto.strategy;

import com.oxiris.travelguide.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 攻略查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StrategyQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 攻略ID
     */
    private Long id;

    /**
     * 攻略标题（模糊搜索）
     */
    private String strategyTitle;

    /**
     * 攻略内容（模糊搜索）
     */
    private String strategyContent;

    /**
     * 审核状态（0-待审核，1-通过，2-拒绝）
     */
    private Integer strategyStatus;

    /**
     * 发布用户ID
     */
    private Long userId;

    /**
     * 攻略标签
     */
    private String strategyTags;

    /**
     * 地点（市级，如"北京"）
     */
    private String location;

    /**
     * 关键词搜索（同时搜索攻略标题和地点名称）
     */
    private String keyword;

    /**
     * 是否为官方推荐（0-否 1-是）
     */
    private Integer isOfficial;
}