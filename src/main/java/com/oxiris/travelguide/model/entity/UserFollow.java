package com.oxiris.travelguide.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_follow")
public class UserFollow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("followerId")
    private Long followerId;

    @Column("followedUserId")
    private Long followedUserId;

    @Column("createTime")
    private LocalDateTime createTime;
}
