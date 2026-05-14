package com.oxiris.travelguide.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息表 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notify")
public class Notify implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 接收用户ID
     */
    @Column("receiverId")
    private Long receiverId;

    /**
     * 发送者ID（系统通知可为空）
     */
    @Column("senderId")
    private Long senderId;

    /**
     * 消息类型（like/comment/collect/system）
     */
    private String type;

    /**
     * 目标类型（1攻略 2评论 3用户）
     */
    @Column("targetType")
    private Integer targetType;

    /**
     * 目标ID
     */
    @Column("targetId")
    private Long targetId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 是否已读（0未读 1已读）
     */
    @Column("isRead")
    private Integer isRead;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

}
