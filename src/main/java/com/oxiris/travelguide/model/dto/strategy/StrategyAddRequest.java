package com.oxiris.travelguide.model.dto.strategy;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 攻略上传请求
 */
@Data
public class StrategyAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

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
    private List<String> imageUrls;

    /**
     * 攻略标签列表
     */
    private List<String> strategyTags;

    /**
     * 地点列表（多选）
     */
    private List<String> locations;
    
    /**
     * 路线规划数据（JSON格式）
     */
    private String routeData;
}