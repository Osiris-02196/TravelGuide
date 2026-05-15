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
 * 举报 实体类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("report")
public class Report implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 举报ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 举报人ID
     */
    @Column("reporterId")
    private Long reporterId;

    /**
     * 举报类型（strategy/comment）
     */
    @Column("targetType")
    private String targetType;

    /**
     * 被举报对象ID
     */
    @Column("targetId")
    private Long targetId;

    /**
     * 被举报用户ID
     */
    @Column("reportedUserId")
    private Long reportedUserId;

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
     * 管理员审核说明
     */
    @Column("reviewRemark")
    private String reviewRemark;

    /**
     * 审核管理员ID
     */
    @Column("reviewAdminId")
    private Long reviewAdminId;

    /**
     * 审核时间
     */
    @Column("reviewTime")
    private LocalDateTime reviewTime;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

}
