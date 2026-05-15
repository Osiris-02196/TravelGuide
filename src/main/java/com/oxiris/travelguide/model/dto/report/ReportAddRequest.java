package com.oxiris.travelguide.model.dto.report;

import lombok.Data;

import java.io.Serializable;

/**
 * 举报添加请求
 */
@Data
public class ReportAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 举报类型（strategy/comment）
     */
    private String targetType;

    /**
     * 被举报对象ID
     */
    private Long targetId;

    /**
     * 被举报用户ID
     */
    private Long reportedUserId;

    /**
     * 举报原因
     */
    private String reason;

    /**
     * 举报详细说明
     */
    private String description;
}
