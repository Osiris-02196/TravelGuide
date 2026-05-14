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

        Comment comment = Comment.builder()
                .userId(loginUser.getId())
                .strategyId(strategyId)
                .content(commentAddRequest.getContent())
                .likeCount(0)
                .commentStatus(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();

        boolean saved = this.save(comment);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR);

        // 更新攻略的评论数
        Strategy strategy = strategyService.getById(strategyId);
        if (strategy != null) {
            strategy.setCommentCount(strategy.getCommentCount() != null ? strategy.getCommentCount() + 1 : 1);
            strategyService.updateById(strategy);
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

        return comment.getId();
    }

    @Override
    public Page<CommentVO> listComments(CommentQueryRequest commentQueryRequest) {
        String strategyIdStr = commentQueryRequest.getStrategyId();
        ThrowUtils.throwIf(strategyIdStr == null || strategyIdStr.trim().isEmpty(), ErrorCode.PARAMS_ERROR);
        Long strategyId = Long.valueOf(strategyIdStr);

        // 使用字符串字段名，风格与 StrategyServiceImpl 一致
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select("comment.id", "comment.userId", "comment.strategyId", "comment.content",
                        "comment.likeCount", "comment.createTime", "user.userName", "user.userAvatar")
                .from("comment")
                .leftJoin("user").on("comment.userId = user.id")
                .where("comment.strategyId = ?", strategyId)
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

        // 使用 MyBatis-Flex 的分页方式（同 StrategyServiceImpl）
        Page<CommentVO> page = this.mapper.paginateAs(Page.of(pageNum, pageSize), queryWrapper, CommentVO.class);
        return page;
    }

    @Override
    public Boolean likeComment(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        Comment comment = this.getById(id);
        ThrowUtils.throwIf(comment == null, ErrorCode.NOT_FOUND_ERROR);

        boolean updated = UpdateChain.of(Comment.class)
                .set(Comment::getLikeCount, comment.getLikeCount() + 1)
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
}