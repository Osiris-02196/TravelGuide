package com.oxiris.travelguide.model.dto.report;

import lombok.Data;

import java.io.Serializable;

/**
 * 举报审核请求
 */
@Data
public class ReportReviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审核状态（approved/rejected）
     */
    private String status;

    /**
     * 管理员审核说明
     */
    private String reviewRemark;
}
