package com.oxiris.travelguide.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.mapper.CommentMapper;
import com.oxiris.travelguide.mapper.ReportMapper;
import com.oxiris.travelguide.mapper.StrategyMapper;
import com.oxiris.travelguide.model.dto.report.ReportAddRequest;
import com.oxiris.travelguide.model.dto.report.ReportQueryRequest;
import com.oxiris.travelguide.model.dto.report.ReportReviewRequest;
import com.oxiris.travelguide.model.entity.Comment;
import com.oxiris.travelguide.model.entity.Report;
import com.oxiris.travelguide.model.entity.Strategy;
import com.oxiris.travelguide.model.entity.User;
import com.oxiris.travelguide.model.enums.report.ReportStatusEnum;
import com.oxiris.travelguide.model.enums.report.ReportTargetTypeEnum;
import com.oxiris.travelguide.model.vo.ReportVO;
import com.oxiris.travelguide.service.CommentService;
import com.oxiris.travelguide.service.NotifyService;
import com.oxiris.travelguide.service.ReportService;
import com.oxiris.travelguide.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 举报 服务层实现。
 */
@Service
@Slf4j
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    @Resource
    private UserService userService;

    @Resource
    private StrategyMapper strategyMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private NotifyService notifyService;

    @Resource
    private CommentService commentService;

    @Override
    public Long addReport(ReportAddRequest reportAddRequest, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(reportAddRequest == null, ErrorCode.PARAMS_ERROR);
        String targetType = reportAddRequest.getTargetType();
        Long targetId = reportAddRequest.getTargetId();
        Long reportedUserId = reportAddRequest.getReportedUserId();
        String reason = reportAddRequest.getReason();
        ThrowUtils.throwIf(StrUtil.isBlank(targetType), ErrorCode.PARAMS_ERROR, "举报类型不能为空");
        ThrowUtils.throwIf(targetId == null || targetId <= 0, ErrorCode.PARAMS_ERROR, "被举报对象ID不能为空");
        ThrowUtils.throwIf(reportedUserId == null || reportedUserId <= 0, ErrorCode.PARAMS_ERROR, "被举报用户ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(reason), ErrorCode.PARAMS_ERROR, "举报原因不能为空");
        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 3. 校验不能举报自己
        ThrowUtils.throwIf(loginUser.getId().equals(reportedUserId),
                ErrorCode.OPERATION_ERROR, "不能举报自己");
        // 4. 校验目标存在，以数据库记录为准确定被举报用户ID
        ReportTargetTypeEnum targetTypeEnum = ReportTargetTypeEnum.getEnumByValue(targetType);
        ThrowUtils.throwIf(targetTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的举报类型");
        switch (targetTypeEnum) {
            case STRATEGY -> {
                Strategy strategy = strategyMapper.selectOneById(targetId);
                if (strategy != null) {
                    reportedUserId = strategy.getUserId();
                } else {
                    validateReportedUserExists(reportedUserId);
                }
            }
            case COMMENT -> {
                Comment comment = commentMapper.selectOneById(targetId);
                if (comment != null) {
                    reportedUserId = comment.getUserId();
                } else {
                    validateReportedUserExists(reportedUserId);
                }
            }
        }
        // 5. 构建举报实体
        Report report = Report.builder()
                .reporterId(loginUser.getId())
                .targetType(targetType)
                .targetId(targetId)
                .reportedUserId(reportedUserId)
                .reason(reason)
                .description(reportAddRequest.getDescription())
                .status(ReportStatusEnum.PENDING.getValue())
                .createTime(LocalDateTime.now())
                .build();
        // 6. 保存举报
        boolean saveResult = this.save(report);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "举报提交失败");
        return report.getId();
    }

    @Override
    public Page<ReportVO> listMyReports(ReportQueryRequest reportQueryRequest, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(reportQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 3. 设置查询当前用户的举报
        reportQueryRequest.setReporterId(loginUser.getId());
        // 4. 分页查询
        long pageNum = reportQueryRequest.getPageNum();
        long pageSize = reportQueryRequest.getPageSize();
        QueryWrapper queryWrapper = buildQueryWrapper(reportQueryRequest);
        Page<Report> reportPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        // 5. 转换VO
        Page<ReportVO> reportVOPage = new Page<>(pageNum, pageSize, reportPage.getTotalRow());
        List<ReportVO> reportVOList = getReportVOList(reportPage.getRecords());
        reportVOPage.setRecords(reportVOList);
        return reportVOPage;
    }

    @Override
    public ReportVO getReportDetail(Long id) {
        // 1. 校验参数
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        // 2. 查询举报
        Report report = this.getById(id);
        ThrowUtils.throwIf(report == null, ErrorCode.NOT_FOUND_ERROR, "举报不存在");
        // 3. 转换VO
        ReportVO reportVO = getReportVO(report);
        // 4. 注入被举报内容摘要和所属攻略信息
        injectTargetInfo(reportVO, report);
        return reportVO;
    }

    @Override
    public Page<ReportVO> listPendingReports(ReportQueryRequest reportQueryRequest) {
        // 1. 校验参数
        ThrowUtils.throwIf(reportQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 2. 不按状态筛选，显示所有举报（待审核 + 已审核）
        // 3. 设置时间排序（降序，最新的优先）
        reportQueryRequest.setSortField("createTime");
        reportQueryRequest.setSortOrder("descend");
        // 4. 分页查询
        long pageNum = reportQueryRequest.getPageNum();
        long pageSize = reportQueryRequest.getPageSize();
        QueryWrapper queryWrapper = buildQueryWrapper(reportQueryRequest);
        Page<Report> reportPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        // 5. 转换VO并注入被举报内容信息
        Page<ReportVO> reportVOPage = new Page<>(pageNum, pageSize, reportPage.getTotalRow());
        List<ReportVO> reportVOList = getReportVOList(reportPage.getRecords());
        for (int i = 0; i < reportVOList.size(); i++) {
            injectTargetInfo(reportVOList.get(i), reportPage.getRecords().get(i));
        }
        reportVOPage.setRecords(reportVOList);
        return reportVOPage;
    }

    @Override
    @Transactional
    public Boolean reviewReport(Long id, ReportReviewRequest reportReviewRequest, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(reportReviewRequest == null, ErrorCode.PARAMS_ERROR);
        String status = reportReviewRequest.getStatus();
        String reviewRemark = reportReviewRequest.getReviewRemark();
        ThrowUtils.throwIf(StrUtil.isBlank(status), ErrorCode.PARAMS_ERROR, "审核状态不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(reviewRemark), ErrorCode.PARAMS_ERROR, "审核说明不能为空");
        ThrowUtils.throwIf(!ReportStatusEnum.APPROVED.getValue().equals(status)
                        && !ReportStatusEnum.REJECTED.getValue().equals(status),
                ErrorCode.PARAMS_ERROR, "审核状态必须为approved(举报成立)或rejected(举报驳回)");
        // 2. 获取当前管理员
        User adminUser = userService.getLoginUser(request);
        // 3. 查询举报
        Report report = this.getById(id);
        ThrowUtils.throwIf(report == null, ErrorCode.NOT_FOUND_ERROR, "举报不存在");
        // 4. 校验当前状态必须为待审核
        ThrowUtils.throwIf(!ReportStatusEnum.PENDING.getValue().equals(report.getStatus()),
                ErrorCode.OPERATION_ERROR, "只能审核待审核状态的举报");
        // 5. 更新举报审核信息
        report.setStatus(status);
        report.setReviewRemark(reviewRemark);
        report.setReviewAdminId(adminUser.getId());
        report.setReviewTime(LocalDateTime.now());
        boolean updateResult = this.updateById(report);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "审核举报失败");
        // 6. 审核通过：通知相关用户，并处理删除被举报内容
        if (ReportStatusEnum.APPROVED.getValue().equals(status)) {
            handleApprovedReport(report);
            // 如果要求删除被举报内容且目标为评论，则删除评论
            Boolean deleteTarget = reportReviewRequest.getDeleteTarget();
            if (Boolean.TRUE.equals(deleteTarget) && ReportTargetTypeEnum.COMMENT.getValue().equals(report.getTargetType())) {
                try {
                    commentService.adminDeleteComment(report.getTargetId());
                } catch (Exception e) {
                    log.warn("删除被举报评论失败: {}", e.getMessage());
                }
            }
        } else {
            // 7. 审核驳回：仅通知举报人
            handleRejectedReport(report);
        }
        return true;
    }

    /**
     * 处理举报成立：通知举报人和被举报用户
     */
    private void handleApprovedReport(Report report) {
        // 1. 通知举报人：举报成立
        try {
            notifyService.createAndPushNotify(report.getReporterId(), null,
                    "report_result", 4, report.getId());
        } catch (Exception e) {
            log.warn("通知举报人失败: {}", e.getMessage());
        }
        // 2. 通知被举报用户：内容被举报违规
        try {
            Integer notifyTargetType = ReportTargetTypeEnum.STRATEGY.getValue().equals(report.getTargetType()) ? 1 : 2;
            notifyService.createAndPushNotify(report.getReportedUserId(), null,
                    "violation", notifyTargetType, report.getId());
        } catch (Exception e) {
            log.warn("通知被举报用户失败: {}", e.getMessage());
        }
    }

    /**
     * 处理举报驳回：仅通知举报人
     */
    private void handleRejectedReport(Report report) {
        try {
            notifyService.createAndPushNotify(report.getReporterId(), null,
                    "report_result", 4, report.getId());
        } catch (Exception e) {
            log.warn("通知举报人失败: {}", e.getMessage());
        }
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper buildQueryWrapper(ReportQueryRequest reportQueryRequest) {
        Long reporterId = reportQueryRequest.getReporterId();
        String targetType = reportQueryRequest.getTargetType();
        String status = reportQueryRequest.getStatus();
        String sortField = reportQueryRequest.getSortField();
        String sortOrder = reportQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create();
        if (reporterId != null) {
            queryWrapper.eq("reporterId", reporterId);
        }
        if (StrUtil.isNotBlank(targetType)) {
            queryWrapper.eq("targetType", targetType);
        }
        if (StrUtil.isNotBlank(status)) {
            queryWrapper.eq("status", status);
        }
        if (StrUtil.isNotBlank(sortField)) {
            boolean isAsc = "ascend".equals(sortOrder);
            queryWrapper.orderBy(sortField, isAsc);
        } else {
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

    /**
     * 将 Report 实体转为 ReportVO
     */
    private ReportVO getReportVO(Report report) {
        if (report == null) {
            return null;
        }
        ReportVO reportVO = new ReportVO();
        BeanUtil.copyProperties(report, reportVO);
        // 注入举报人名称
        if (report.getReporterId() != null) {
            User reporter = userService.getById(report.getReporterId());
            if (reporter != null) {
                reportVO.setReporterName(reporter.getUserName());
            }
        }
        // 注入被举报用户名称
        if (report.getReportedUserId() != null) {
            User reportedUser = userService.getById(report.getReportedUserId());
            if (reportedUser != null) {
                reportVO.setReportedUserName(reportedUser.getUserName());
            }
        }
        // 注入审核管理员名称
        if (report.getReviewAdminId() != null) {
            User reviewAdmin = userService.getById(report.getReviewAdminId());
            if (reviewAdmin != null) {
                reportVO.setReviewAdminName(reviewAdmin.getUserName());
            }
        }
        return reportVO;
    }

    /**
     * 批量转换 Report -> ReportVO
     */
    private List<ReportVO> getReportVOList(List<Report> reportList) {
        if (CollUtil.isEmpty(reportList)) {
            return new ArrayList<>();
        }
        return reportList.stream().map(this::getReportVO).collect(Collectors.toList());
    }

    /**
     * 注入被举报内容摘要和所属攻略信息到 ReportVO
     */
    private void injectTargetInfo(ReportVO reportVO, Report report) {
        String targetType = report.getTargetType();
        Long targetId = report.getTargetId();
        if (ReportTargetTypeEnum.STRATEGY.getValue().equals(targetType)) {
            Strategy strategy = strategyMapper.selectByIdIncludeDeleted(targetId);
            if (strategy != null) {
                reportVO.setTargetContent(strategy.getStrategyTitle());
                reportVO.setStrategyId(strategy.getId());
                reportVO.setStrategyTitle(strategy.getStrategyTitle());
            }
        } else if (ReportTargetTypeEnum.COMMENT.getValue().equals(targetType)) {
            Comment comment = commentMapper.selectByIdIncludeDeleted(targetId);
            if (comment != null) {
                String content = comment.getContent();
                reportVO.setTargetContent(StrUtil.isNotBlank(content) && content.length() > 100
                        ? content.substring(0, 100) + "..." : content);
                reportVO.setStrategyId(comment.getStrategyId());
                // 查询评论所属攻略标题
                if (comment.getStrategyId() != null) {
                    Strategy strategy = strategyMapper.selectByIdIncludeDeleted(comment.getStrategyId());
                    if (strategy != null) {
                        reportVO.setStrategyTitle(strategy.getStrategyTitle());
                    }
                }
            }
        }
    }

    /**
     * 验证被举报用户存在
     */
    private void validateReportedUserExists(Long reportedUserId) {
        User reportedUser = userService.getById(reportedUserId);
        ThrowUtils.throwIf(reportedUser == null, ErrorCode.NOT_FOUND_ERROR, "被举报用户不存在");
    }
}
