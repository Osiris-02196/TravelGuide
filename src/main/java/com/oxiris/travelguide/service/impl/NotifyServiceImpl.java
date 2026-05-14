package com.oxiris.travelguide.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.mapper.NotifyMapper;
import com.oxiris.travelguide.model.dto.notify.NotifyQueryRequest;
import com.oxiris.travelguide.model.entity.Notify;
import com.oxiris.travelguide.model.entity.User;
import com.oxiris.travelguide.model.entity.Strategy;
import com.oxiris.travelguide.model.vo.NotifyVO;
import com.oxiris.travelguide.service.NotifyService;
import com.oxiris.travelguide.service.UserService;
import com.oxiris.travelguide.service.StrategyService;
import com.oxiris.travelguide.websocket.NotifyWebSocketHandler;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息表 服务层实现。
 *
 */
@Service
@Slf4j
public class NotifyServiceImpl extends ServiceImpl<NotifyMapper, Notify> implements NotifyService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private StrategyService strategyService;

    @Resource
    private NotifyWebSocketHandler notifyWebSocketHandler;

    @Override
    public Notify createAndPushNotify(Long receiverId, Long senderId, String type, Integer targetType, Long targetId) {
        // Step 1: 构建消息实体
        Notify notify = new Notify();
        notify.setReceiverId(receiverId);
        notify.setSenderId(senderId);
        notify.setType(type);
        notify.setTargetType(targetType);
        notify.setTargetId(targetId);
        notify.setIsRead(0);
        // Step 2: 动态拼接消息内容
        String content = buildNotifyContent(notify);
        notify.setContent(content);
        // Step 3: 保存消息到数据库
        boolean saved = this.save(notify);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "消息保存失败");
        // Step 4: 通过 WebSocket 实时推送
        notifyWebSocketHandler.pushNotify(notify);
        return notify;
    }

    @Override
    public String buildNotifyContent(Notify notify) {
        // Step 1: 获取发送者名称
        String senderName = "系统";
        if (notify.getSenderId() != null) {
            User sender = userService.getById(notify.getSenderId());
            if (sender != null && StrUtil.isNotBlank(sender.getUserName())) {
                senderName = sender.getUserName();
            }
        }
        // Step 2: 根据消息类型和目标类型拼接内容
        String type = notify.getType();
        Integer targetType = notify.getTargetType();
        StringBuilder content = new StringBuilder();
        if ("system".equals(type)) {
            // 系统通知
            if (targetType != null && targetType == 1) {
                // 攻略审核通知
                Strategy strategy = strategyService.getById(notify.getTargetId());
                String strategyTitle = (strategy != null && StrUtil.isNotBlank(strategy.getStrategyTitle()))
                        ? strategy.getStrategyTitle() : "未知攻略";
                content.append("你的攻略「").append(strategyTitle).append("」审核状态已更新");
            } else if (targetType != null && targetType == 3) {
                // 用户状态变更通知
                content.append("你的账号状态已变更");
            } else {
                content.append("系统通知");
            }
        } else if ("like".equals(type)) {
            if (targetType != null && targetType == 1) {
                content.append("你的攻略被").append(senderName).append("点赞");
            } else if (targetType != null && targetType == 2) {
                content.append("你的评论被").append(senderName).append("点赞");
            } else {
                content.append(senderName).append("给你点了个赞");
            }
        } else if ("comment".equals(type)) {
            if (targetType != null && targetType == 1) {
                content.append("你的攻略被").append(senderName).append("评论");
            } else if (targetType != null && targetType == 2) {
                content.append("你的评论被").append(senderName).append("回复");
            } else {
                content.append(senderName).append("评论了你");
            }
        } else if ("collect".equals(type)) {
            content.append("你的攻略被").append(senderName).append("收藏");
        } else if ("pending".equals(type)) {
            // 管理员待审核通知
            content.append(senderName).append("的攻略待审核");
        } else {
            content.append("你收到一条新消息");
        }
        return content.toString();
    }

    @Override
    public NotifyVO getNotifyVO(Notify notify) {
        // Step 1: 参数校验
        if (notify == null) {
            return null;
        }
        // Step 2: Bean 转换（使用 Hutool）
        NotifyVO notifyVO = new NotifyVO();
        BeanUtil.copyProperties(notify, notifyVO);
        // Step 3: 注入发送者名称
        if (notify.getSenderId() != null) {
            User sender = userService.getById(notify.getSenderId());
            if (sender != null) {
                notifyVO.setSenderName(sender.getUserName());
            }
        } else {
            notifyVO.setSenderName("系统");
        }
        // Step 4: 确保 content 已拼接
        if (StrUtil.isBlank(notifyVO.getContent())) {
            notifyVO.setContent(buildNotifyContent(notify));
        }
        return notifyVO;
    }

    @Override
    public List<NotifyVO> getNotifyVOList(List<Notify> notifyList) {
        // Step 1: 参数校验
        if (CollUtil.isEmpty(notifyList)) {
            return new ArrayList<>();
        }
        // Step 2: 批量转换
        return notifyList.stream().map(this::getNotifyVO).collect(Collectors.toList());
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper buildQueryWrapper(NotifyQueryRequest notifyQueryRequest) {
        Long receiverId = notifyQueryRequest.getReceiverId();
        String type = notifyQueryRequest.getType();
        Integer isRead = notifyQueryRequest.getIsRead();
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (receiverId != null) {
            queryWrapper.eq("receiverId", receiverId);
        }
        if (StrUtil.isNotBlank(type)) {
            queryWrapper.eq("type", type);
        }
        if (isRead != null) {
            queryWrapper.eq("isRead", isRead);
        }
        queryWrapper.orderBy("createTime", false);
        return queryWrapper;
    }

    @Override
    public Page<NotifyVO> listMyNotifies(NotifyQueryRequest notifyQueryRequest, HttpServletRequest request) {
        // Step 1: 校验参数
        ThrowUtils.throwIf(notifyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // Step 2: 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // Step 3: 设置查询当前用户的接收消息
        notifyQueryRequest.setReceiverId(loginUser.getId());
        // Step 4: 校验分页参数（每页最多20条）
        long pageNum = notifyQueryRequest.getPageNum();
        long pageSize = Math.min(notifyQueryRequest.getPageSize(), 20);
        if (pageSize <= 0) {
            pageSize = 10;
        }
        // Step 5: 分页查询
        QueryWrapper queryWrapper = buildQueryWrapper(notifyQueryRequest);
        Page<Notify> notifyPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        // Step 6: 转换VO
        Page<NotifyVO> notifyVOPage = new Page<>(pageNum, pageSize, notifyPage.getTotalRow());
        List<NotifyVO> notifyVOList = this.getNotifyVOList(notifyPage.getRecords());
        notifyVOPage.setRecords(notifyVOList);
        return notifyVOPage;
    }

    @Override
    public Long getUnreadCount(HttpServletRequest request) {
        // Step 1: 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // Step 2: 查询未读数量
        return this.count(QueryWrapper.create()
                .eq("receiverId", loginUser.getId())
                .eq("isRead", 0));
    }

    @Override
    public Boolean readAll(HttpServletRequest request) {
        // Step 1: 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // Step 2: 查询所有未读消息
        List<Notify> unreadList = this.list(QueryWrapper.create()
                .eq("receiverId", loginUser.getId())
                .eq("isRead", 0));
        // Step 3: 逐个标记为已读
        if (CollUtil.isNotEmpty(unreadList)) {
            for (Notify notify : unreadList) {
                notify.setIsRead(1);
            }
            boolean result = this.updateBatch(unreadList);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "标记已读失败");
        }
        // Step 4: 推送未读数量更新
        notifyWebSocketHandler.pushUnreadCountUpdate(loginUser.getId(), 0L);
        return true;
    }

    @Override
    public Page<NotifyVO> listAdminNotifies(NotifyQueryRequest notifyQueryRequest) {
        // Step 1: 校验参数
        ThrowUtils.throwIf(notifyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // Step 2: 查找所有管理员用户ID
        List<User> adminUsers = userService.list(QueryWrapper.create().in("userRole", "admin", "superadmin"));
        List<Long> adminIds = adminUsers.stream().map(User::getId).collect(Collectors.toList());
        // Step 3: 校验分页参数
        long pageNum = notifyQueryRequest.getPageNum();
        long pageSize = Math.min(notifyQueryRequest.getPageSize(), 20);
        if (pageSize <= 0) {
            pageSize = 10;
        }
        // Step 4: 分页查询（接收者为任意管理员的消息，且类型为 pending）
        QueryWrapper queryWrapper = QueryWrapper.create()
                .in("receiverId", adminIds.toArray())
                .eq("type", "pending")
                .orderBy("createTime", false);
        Page<Notify> notifyPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        // Step 5: 转换VO
        Page<NotifyVO> notifyVOPage = new Page<>(pageNum, pageSize, notifyPage.getTotalRow());
        List<NotifyVO> notifyVOList = this.getNotifyVOList(notifyPage.getRecords());
        notifyVOPage.setRecords(notifyVOList);
        return notifyVOPage;
    }

    @Override
    public Long getAdminUnreadCount() {
        // Step 1: 查找所有管理员用户ID
        List<User> adminUsers = userService.list(QueryWrapper.create().in("userRole", "admin", "superadmin"));
        if (CollUtil.isEmpty(adminUsers)) {
            return 0L;
        }
        List<Long> adminIds = adminUsers.stream().map(User::getId).collect(Collectors.toList());
        // Step 2: 查询未读 pending 消息数量
        return this.count(QueryWrapper.create()
                .in("receiverId", adminIds.toArray())
                .eq("type", "pending")
                .eq("isRead", 0));
    }

    @Override
    public Boolean readAllAdmin() {
        // Step 1: 查找所有管理员用户ID
        List<User> adminUsers = userService.list(QueryWrapper.create().in("userRole", "admin", "superadmin"));
        if (CollUtil.isEmpty(adminUsers)) {
            return true;
        }
        List<Long> adminIds = adminUsers.stream().map(User::getId).collect(Collectors.toList());
        // Step 2: 查询所有未读 pending 消息
        List<Notify> unreadList = this.list(QueryWrapper.create()
                .in("receiverId", adminIds.toArray())
                .eq("type", "pending")
                .eq("isRead", 0));
        // Step 3: 逐个标记为已读
        if (CollUtil.isNotEmpty(unreadList)) {
            for (Notify notify : unreadList) {
                notify.setIsRead(1);
            }
            boolean result = this.updateBatch(unreadList);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "标记已读失败");
        }
        // Step 4: 推送未读数量更新给每个管理员
        for (Long adminId : adminIds) {
            notifyWebSocketHandler.pushUnreadCountUpdate(adminId, 0L);
        }
        return true;
    }
}