package com.oxiris.travelguide.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 地点信息视图（用于下拉框数据展示）
 */
@Data
public class LocationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 地点ID
     */
    private Long id;

    /**
     * 地点编码
     */
    private String locationCode;

    /**
     * 地点名称
     */
    private String locationName;

    /**
     * 父级地点ID（0表示顶级）
     */
    private Long parentId;

    /**
     * 层级（1-国家 2-省/直辖市 3-地级市）
     */
    private Integer level;

    /**
     * 点赞量
     */
    private Integer likeCount;
}