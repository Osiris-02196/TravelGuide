package com.oxiris.travelguide.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.oxiris.travelguide.model.dto.report.ReportAddRequest;
import com.oxiris.travelguide.model.dto.report.ReportQueryRequest;
import com.oxiris.travelguide.model.dto.report.ReportReviewRequest;
import com.oxiris.travelguide.model.entity.Report;
import com.oxiris.travelguide.model.vo.ReportVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 举报 服务层。
 */
public interface ReportService extends IService<Report> {

    /**
     * 用户提交举报
     *
     * @param reportAddRequest 举报请求
     * @param request          HTTP请求
     * @return 举报ID
     */
    Long addReport(ReportAddRequest reportAddRequest, HttpServletRequest request);

    /**
     * 用户分页查询自己的举报列表
     *
     * @param reportQueryRequest 查询请求
     * @param request            HTTP请求
     * @return 分页举报VO
     */
    Page<ReportVO> listMyReports(ReportQueryRequest reportQueryRequest, HttpServletRequest request);

    /**
     * 获取举报详情（含被举报内容和所属攻略信息）
     *
     * @param id 举报ID
     * @return 举报VO
     */
    ReportVO getReportDetail(Long id);

    /**
     * 管理员分页查询待审核的举报列表
     *
     * @param reportQueryRequest 查询请求
     * @return 分页举报VO
     */
    Page<ReportVO> listPendingReports(ReportQueryRequest reportQueryRequest);

    /**
     * 管理员审核举报
     *
     * @param id                 举报ID
     * @param reportReviewRequest 审核请求
     * @param request            HTTP请求
     * @return 是否成功
     */
    Boolean reviewReport(Long id, ReportReviewRequest reportReviewRequest, HttpServletRequest request);
}
