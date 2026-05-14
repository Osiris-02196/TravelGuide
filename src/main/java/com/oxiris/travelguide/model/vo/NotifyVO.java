package com.oxiris.travelguide.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息视图（用于展示）
 */
@Data
public class NotifyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 接收用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiverId;

    /**
     * 发送者ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 目标类型（1攻略 2评论 3用户）
     */
    private Integer targetType;

    /**
     * 目标ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    /**
     * 消息内容（动态拼接）
     */
    private String content;

    /**
     * 是否已读（0未读 1已读）
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}