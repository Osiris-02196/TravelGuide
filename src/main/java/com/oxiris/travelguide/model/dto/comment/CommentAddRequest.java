package com.oxiris.travelguide.model.dto.comment;

import lombok.Data;

import java.io.Serializable;

/**
 * 评论添加请求
 */
@Data
public class CommentAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 攻略ID
     */
    private String strategyId;

    /**
     * 评论内容
     */
    private String content;
}