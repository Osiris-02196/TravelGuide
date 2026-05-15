package com.oxiris.travelguide.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 举报视图
 */
@Data
public class ReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 举报ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 举报人ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reporterId;

    /**
     * 举报人名称
     */
    private String reporterName;

    /**
     * 举报类型（strategy/comment）
     */
    private String targetType;

    /**
     * 被举报对象ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    /**
     * 被举报用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reportedUserId;

    /**
     * 被举报用户名称
     */
    private String reportedUserName;

    /**
     * 举报原因
     */
    private String reason;

    /**
     * 举报详细说明
     */
    private String description;

    /**
     * 举报状态（pending/approved/rejected）
     */
    private String status;

    /**
     * 被举报内容摘要
     */
    private String targetContent;

    /**
     * 被举报内容所属攻略ID（仅targetType=comment时有值）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long strategyId;

    /**
     * 被举报内容所属攻略标题（仅targetType=comment时有值）
     */
    private String strategyTitle;

    /**
     * 管理员审核说明
     */
    private String reviewRemark;

    /**
     * 审核管理员ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reviewAdminId;

    /**
     * 审核管理员名称
     */
    private String reviewAdminName;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
