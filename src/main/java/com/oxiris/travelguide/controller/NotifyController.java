package com.oxiris.travelguide.controller;

import com.mybatisflex.core.paginate.Page;
import com.oxiris.travelguide.annotation.AuthCheck;
import com.oxiris.travelguide.common.BaseResponse;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.common.ResultUtils;
import com.oxiris.travelguide.constant.UserConstant;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.model.dto.notify.NotifyQueryRequest;
import com.oxiris.travelguide.model.vo.NotifyVO;
import com.oxiris.travelguide.service.NotifyService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 消息表 控制层。
 *
 */
@RestController
@RequestMapping("/notify")
public class NotifyController {

    @Resource
    private NotifyService notifyService;

    /**
     * 用户分页查询自己的消息列表（按时间倒序，每页最多20条）
     *
     * @param notifyQueryRequest 查询请求
     * @param request            HTTP请求
     * @return 分页消息VO
     */
    @PostMapping("/list/my")
    public BaseResponse<Page<NotifyVO>> listMyNotifies(@RequestBody NotifyQueryRequest notifyQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(notifyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<NotifyVO> result = notifyService.listMyNotifies(notifyQueryRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前用户未读消息数量
     *
     * @param request HTTP请求
     * @return 未读数量
     */
    @GetMapping("/unread/count")
    public BaseResponse<Long> getUnreadCount(HttpServletRequest request) {
        Long count = notifyService.getUnreadCount(request);
        return ResultUtils.success(count);
    }

    /**
     * 用户一键全部已读
     *
     * @param request HTTP请求
     * @return 是否成功
     */
    @PostMapping("/read/all")
    public BaseResponse<Boolean> readAll(HttpServletRequest request) {
        Boolean result = notifyService.readAll(request);
        return ResultUtils.success(result);
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员分页查询待审核消息列表（按时间倒序，每页最多20条）
     *
     * @param notifyQueryRequest 查询请求
     * @return 分页消息VO
     */
    @PostMapping("/admin/list")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Page<NotifyVO>> listAdminNotifies(@RequestBody NotifyQueryRequest notifyQueryRequest) {
        ThrowUtils.throwIf(notifyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<NotifyVO> result = notifyService.listAdminNotifies(notifyQueryRequest);
        return ResultUtils.success(result);
    }

    /**
     * 获取管理员未读消息数量
     *
     * @return 未读数量
     */
    @GetMapping("/admin/unread/count")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Long> getAdminUnreadCount() {
        Long count = notifyService.getAdminUnreadCount();
        return ResultUtils.success(count);
    }

    /**
     * 管理员一键全部已读
     *
     * @return 是否成功
     */
    @PostMapping("/admin/read/all")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Boolean> readAllAdmin() {
        Boolean result = notifyService.readAllAdmin();
        return ResultUtils.success(result);
    }
}