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
 * 评论表 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("comment")
public class Comment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户ID
     */
    @Column("userId")
    private Long userId;

    /**
     * 攻略ID
     */
    @Column("strategyId")
    private Long strategyId;

    /**
     * 所属一级评论ID（NULL表示顶级评论）
     */
    @Column("parentId")
    private Long parentId;

    /**
     * 被回复用户ID
     */
    @Column("replyToUserId")
    private Long replyToUserId;

    /**
     * 评论内容
     */
    private String content;

    @Column("likeCount")
    private Integer likeCount;

    /**
     * 状态（1正常，0违规）
     */
    @Column("commentStatus")
    private Integer commentStatus;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除（0-否 1-是）
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
