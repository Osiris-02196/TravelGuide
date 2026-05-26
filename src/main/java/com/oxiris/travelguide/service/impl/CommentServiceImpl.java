package com.oxiris.travelguide.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.model.dto.comment.CommentAddRequest;
import com.oxiris.travelguide.model.dto.comment.CommentQueryRequest;
import com.oxiris.travelguide.model.entity.Comment;
import com.oxiris.travelguide.model.entity.User;
import com.oxiris.travelguide.model.vo.CommentVO;
import com.oxiris.travelguide.mapper.CommentMapper;
import com.oxiris.travelguide.service.CommentService;
import com.oxiris.travelguide.service.UserService;
import com.oxiris.travelguide.service.StrategyService;
import com.oxiris.travelguide.service.NotifyService;
import com.oxiris.travelguide.model.entity.Strategy;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 评论表 服务层实现。
 */
@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Resource
    private UserService userService;

    @Resource
    private StrategyService strategyService;

    @Resource
    private NotifyService notifyService;

    @Override
    public Long addComment(CommentAddRequest commentAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        String strategyIdStr = commentAddRequest.getStrategyId();
        ThrowUtils.throwIf(strategyIdStr == null || strategyIdStr.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "攻略ID不能为空");
        Long strategyId = Long.valueOf(strategyIdStr);

        // 处理回复
        Long resolvedParentId = null;
        Long resolvedReplyToUserId = null;
        String parentIdStr = commentAddRequest.getParentId();
        if (StringUtils.hasText(parentIdStr)) {
            Long rawParentId = Long.valueOf(parentIdStr);
            Comment targetComment = this.getById(rawParentId);
            ThrowUtils.throwIf(targetComment == null, ErrorCode.NOT_FOUND_ERROR, "被回复的评论不存在");

            // 如果目标本身是回复，则关联到其所属的一级评论
            resolvedParentId = targetComment.getParentId() != null ? targetComment.getParentId() : targetComment.getId();

            String replyToUserIdStr = commentAddRequest.getReplyToUserId();
            if (StringUtils.hasText(replyToUserIdStr)) {
                resolvedReplyToUserId = Long.valueOf(replyToUserIdStr);
            }
        }

        Comment comment = Comment.builder()
                .userId(loginUser.getId())
                .strategyId(strategyId)
                .parentId(resolvedParentId)
                .replyToUserId(resolvedReplyToUserId)
                .content(commentAddRequest.getContent())
                .likeCount(0)
                .commentStatus(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();

        boolean saved = this.save(comment);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR);

        // 原子更新攻略的评论数和热度分
        Strategy strategy = strategyService.getById(strategyId);
        if (strategy != null) {
            UpdateChain.of(Strategy.class)
                    .setRaw(Strategy::getCommentCount, "COALESCE(commentCount, 0) + 1")
                    .setRaw(Strategy::getHotScore, StrategyServiceImpl.hotScoreSql(
                            "COALESCE(clickCount, 0)",
                            "COALESCE(likeCount, 0)",
                            "COALESCE(collectCount, 0)",
                            "COALESCE(commentCount, 0) + 1"
                    ))
                    .where(Strategy::getId).eq(strategyId)
                    .update();
            // 发送通知给攻略作者
            if (!strategy.getUserId().equals(loginUser.getId())) {
                try {
                    notifyService.createAndPushNotify(strategy.getUserId(), loginUser.getId(),
                            "comment", 1, strategyId);
                } catch (Exception e) {
                    log.warn("发送评论通知失败: {}", e.getMessage());
                }
            }
        }

        // 发送回复通知给被回复用户
        if (resolvedReplyToUserId != null && !resolvedReplyToUserId.equals(loginUser.getId())) {
            try {
                notifyService.createAndPushNotify(resolvedReplyToUserId, loginUser.getId(),
                        "comment", 2, comment.getId());
            } catch (Exception e) {
                log.warn("发送回复通知失败: {}", e.getMessage());
            }
        }

        return comment.getId();
    }

    @Override
    public Page<CommentVO> listComments(CommentQueryRequest commentQueryRequest) {
        String strategyIdStr = commentQueryRequest.getStrategyId();
        ThrowUtils.throwIf(strategyIdStr == null || strategyIdStr.trim().isEmpty(), ErrorCode.PARAMS_ERROR);
        Long strategyId = Long.valueOf(strategyIdStr);

        // 只查询一级评论（parentId IS NULL），并带上回复数子查询
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select("comment.id", "comment.userId", "comment.strategyId", "comment.content",
                        "comment.likeCount", "comment.createTime", "comment.parentId", "comment.replyToUserId",
                        "user.userName", "user.userAvatar",
                        "(SELECT COUNT(*) FROM comment r WHERE r.parentId = comment.id AND r.isDelete = 0 AND r.commentStatus = 1) AS replyCount")
                .from("comment")
                .leftJoin("user").on("comment.userId = user.id")
                .where("comment.strategyId = ?", strategyId)
                .and("comment.parentId IS NULL")
                .and("comment.isDelete = 0")
                .and("comment.commentStatus = 1");

        String sortField = commentQueryRequest.getSortField();
        String sortOrder = commentQueryRequest.getSortOrder();
        if ("likeCount".equals(sortField)) {
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
            queryWrapper.orderBy("comment.likeCount", isAsc);
        } else {
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
            queryWrapper.orderBy("comment.createTime", isAsc);
        }

        long pageNum = commentQueryRequest.getPageNum();
        long pageSize = commentQueryRequest.getPageSize();
        if (pageNum <= 0) pageNum = 1;
        if (pageSize <= 0) pageSize = 10;

        return this.mapper.paginateAs(Page.of(pageNum, pageSize), queryWrapper, CommentVO.class);
    }

    @Override
    public Page<CommentVO> listReplies(Long parentId, int pageNum, int pageSize) {
        ThrowUtils.throwIf(parentId == null || parentId <= 0, ErrorCode.PARAMS_ERROR);

        QueryWrapper queryWrapper = QueryWrapper.create()
                .select("comment.id", "comment.userId", "comment.strategyId", "comment.content",
                        "comment.likeCount", "comment.createTime", "comment.parentId", "comment.replyToUserId",
                        "user.userName", "user.userAvatar",
                        "replyUser.userName AS replyToUserName")
                .from("comment")
                .leftJoin("user").on("comment.userId = user.id")
                .leftJoin("user").as("replyUser").on("comment.replyToUserId = replyUser.id")
                .where("comment.parentId = ?", parentId)
                .and("comment.isDelete = 0")
                .and("comment.commentStatus = 1")
                .orderBy("comment.createTime", true);

        if (pageNum <= 0) pageNum = 1;
        if (pageSize <= 0) pageSize = 5;

        return this.mapper.paginateAs(Page.of(pageNum, pageSize), queryWrapper, CommentVO.class);
    }

    @Override
    public Boolean likeComment(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        Comment comment = this.getById(id);
        ThrowUtils.throwIf(comment == null, ErrorCode.NOT_FOUND_ERROR);

        boolean updated = UpdateChain.of(Comment.class)
                .setRaw(Comment::getLikeCount, "likeCount + 1")
                .where(Comment::getId).eq(id)
                .update();
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "点赞失败");

        // 发送通知给评论作者
        if (!comment.getUserId().equals(loginUser.getId())) {
            try {
                notifyService.createAndPushNotify(comment.getUserId(), loginUser.getId(),
                        "like", 2, id);
            } catch (Exception e) {
                log.warn("发送评论点赞通知失败: {}", e.getMessage());
            }
        }
        return true;
    }

    @Override
    public void adminDeleteComment(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Comment comment = this.getById(id);
        ThrowUtils.throwIf(comment == null, ErrorCode.NOT_FOUND_ERROR, "评论不存在");

        // 如果是一级评论，联动删除其下的回复
        if (comment.getParentId() == null) {
            this.remove(QueryWrapper.create().where("parentId = ?", id));
        }

        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR, "删除评论失败");
    }
}
