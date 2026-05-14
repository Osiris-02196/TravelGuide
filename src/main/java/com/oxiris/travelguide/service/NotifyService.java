package com.oxiris.travelguide.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.oxiris.travelguide.model.dto.notify.NotifyQueryRequest;
import com.oxiris.travelguide.model.entity.Notify;
import com.oxiris.travelguide.model.vo.NotifyVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 消息表 服务层接口。
 *
 */
public interface NotifyService extends IService<Notify> {

    /**
     * 创建并保存一条消息通知，同时通过 WebSocket 推送给接收者
     *
     * @param receiverId 接收用户ID
     * @param senderId   发送者ID（系统通知可为 null）
     * @param type       消息类型
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 创建的消息实体
     */
    Notify createAndPushNotify(Long receiverId, Long senderId, String type, Integer targetType, Long targetId);

    /**
     * 用户分页查询自己的消息列表（按时间倒序）
     *
     * @param notifyQueryRequest 查询请求
     * @param request            HTTP请求
     * @return 分页消息VO
     */
    Page<NotifyVO> listMyNotifies(NotifyQueryRequest notifyQueryRequest, HttpServletRequest request);

    /**
     * 获取当前用户未读消息数量
     *
     * @param request HTTP请求
     * @return 未读数量
     */
    Long getUnreadCount(HttpServletRequest request);

    /**
     * 一键全部已读（当前用户所有未读消息标记为已读）
     *
     * @param request HTTP请求
     * @return 是否成功
     */
    Boolean readAll(HttpServletRequest request);

    /**
     * 管理员分页查询待审核相关的消息列表（按时间倒序）
     *
     * @param notifyQueryRequest 查询请求
     * @return 分页消息VO
     */
    Page<NotifyVO> listAdminNotifies(NotifyQueryRequest notifyQueryRequest);

    /**
     * 获取管理员未读消息数量
     *
     * @return 未读数量
     */
    Long getAdminUnreadCount();

    /**
     * 管理员一键全部已读
     *
     * @return 是否成功
     */
    Boolean readAllAdmin();

    /**
     * 根据消息内容和关联用户动态拼接 content 字段
     *
     * @param notify 消息实体
     * @return 拼接后的消息内容
     */
    String buildNotifyContent(Notify notify);

    /**
     * 将 Notify 实体转为 NotifyVO
     *
     * @param notify 消息实体
     * @return 消息VO
     */
    NotifyVO getNotifyVO(Notify notify);

    /**
     * 批量将 Notify 实体转为 NotifyVO
     *
     * @param notifyList 消息实体列表
     * @return 消息VO列表
     */
    List<NotifyVO> getNotifyVOList(List<Notify> notifyList);
}