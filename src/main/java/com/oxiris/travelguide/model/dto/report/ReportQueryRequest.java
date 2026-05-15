package com.oxiris.travelguide.model.dto.report;

import com.oxiris.travelguide.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 举报查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ReportQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 举报人ID
     */
    private Long reporterId;

    /**
     * 举报类型（strategy/comment）
     */
    private String targetType;

    /**
     * 举报状态（pending/approved/rejected）
     */
    private String status;
}
