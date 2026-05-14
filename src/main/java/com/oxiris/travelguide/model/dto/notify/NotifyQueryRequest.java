package com.oxiris.travelguide.model.dto.notify;

import com.oxiris.travelguide.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 消息查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class NotifyQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 接收用户ID
     */
    private Long receiverId;

    /**
     * 消息类型（like/comment/collect/system）
     */
    private String type;

    /**
     * 是否已读（0未读 1已读），不传则查全部
     */
    private Integer isRead;
}