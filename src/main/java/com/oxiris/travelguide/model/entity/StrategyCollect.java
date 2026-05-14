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
 * 攻略收藏表 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("strategy_collect")
public class StrategyCollect implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
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
     * 收藏时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

}
