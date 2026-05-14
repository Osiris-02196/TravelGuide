package com.oxiris.travelguide.model.dto.comment;

import com.oxiris.travelguide.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 评论查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommentQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 攻略ID
     */
    private String strategyId;

    /**
     * 排序字段：likeCount（最热）或 createTime（最新）
     */
    private String sortField;

    /**
     * 排序顺序：asc/desc
     */
    private String sortOrder;
}