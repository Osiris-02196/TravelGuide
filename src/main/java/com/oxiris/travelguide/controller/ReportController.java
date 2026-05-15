package com.oxiris.travelguide.controller;

import com.mybatisflex.core.paginate.Page;
import com.oxiris.travelguide.annotation.AuthCheck;
import com.oxiris.travelguide.annotation.StatusCheck;
import com.oxiris.travelguide.common.BaseResponse;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.common.ResultUtils;
import com.oxiris.travelguide.constant.UserConstant;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.model.dto.report.ReportAddRequest;
import com.oxiris.travelguide.model.dto.report.ReportQueryRequest;
import com.oxiris.travelguide.model.dto.report.ReportReviewRequest;
import com.oxiris.travelguide.model.vo.ReportVO;
import com.oxiris.travelguide.service.ReportService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 举报 控制层。
 */
@RestController
@RequestMapping("/report")
public class ReportController {

    @Resource
    private ReportService reportService;

    /**
     * 用户提交举报
     *
     * @param reportAddRequest 举报请求
     * @param request          HTTP请求
     * @return 举报ID
     */
    @PostMapping("/add")
    @StatusCheck(allowedStatus = {UserConstant.NORMAL, UserConstant.MUTED})
    public BaseResponse<Long> addReport(@RequestBody ReportAddRequest reportAddRequest,
                                        HttpServletRequest request) {
        ThrowUtils.throwIf(reportAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = reportService.addReport(reportAddRequest, request);
        return ResultUtils.success(id);
    }

    /**
     * 用户分页查询自己的举报列表
     *
     * @param reportQueryRequest 查询请求
     * @param request            HTTP请求
     * @return 分页举报VO
     */
    @PostMapping("/list/my")
    public BaseResponse<Page<ReportVO>> listMyReports(@RequestBody ReportQueryRequest reportQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(reportQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<ReportVO> result = reportService.listMyReports(reportQueryRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 获取举报详情（包含被举报内容和所属攻略信息）
     *
     * @param id 举报ID
     * @return 举报VO
     */
    @GetMapping("/detail/{id}")
    public BaseResponse<ReportVO> getReportDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ReportVO reportVO = reportService.getReportDetail(id);
        return ResultUtils.success(reportVO);
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员分页查询待审核的举报列表
     *
     * @param reportQueryRequest 查询请求
     * @return 分页举报VO
     */
    @PostMapping("/admin/list/pending")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Page<ReportVO>> listPendingReports(@RequestBody ReportQueryRequest reportQueryRequest) {
        ThrowUtils.throwIf(reportQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<ReportVO> result = reportService.listPendingReports(reportQueryRequest);
        return ResultUtils.success(result);
    }

    /**
     * 管理员审核举报
     *
     * @param id                 举报ID
     * @param reportReviewRequest 审核请求
     * @param request            HTTP请求
     * @return 是否成功
     */
    @PutMapping("/admin/review/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Boolean> reviewReport(@PathVariable Long id,
                                               @RequestBody ReportReviewRequest reportReviewRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(reportReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Boolean result = reportService.reviewReport(id, reportReviewRequest, request);
        return ResultUtils.success(result);
    }
}
