package com.oxiris.travelguide.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI聊天消息表 实体类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("ai_chat_message")
public class AiChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("sessionId")
    private String sessionId;

    @Column("role")
    private String role;

    @Column("content")
    private String content;

    @Column("createTime")
    private LocalDateTime createTime;
}
