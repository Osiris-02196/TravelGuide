package com.oxiris.travelguide.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.oxiris.travelguide.model.dto.comment.CommentAddRequest;
import com.oxiris.travelguide.model.dto.comment.CommentQueryRequest;
import com.oxiris.travelguide.model.entity.Comment;
import com.oxiris.travelguide.model.vo.CommentVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 评论表 服务层。
 */
public interface CommentService extends IService<Comment> {

    /**
     * 添加评论
     */
    Long addComment(CommentAddRequest commentAddRequest, HttpServletRequest request);

    /**
     * 分页查询评论列表
     */
    Page<CommentVO> listComments(CommentQueryRequest commentQueryRequest);

    /**
     * 点赞评论
     */
    Boolean likeComment(Long id, HttpServletRequest request);

    /**
     * 管理员删除评论（逻辑删除）
     */
    void adminDeleteComment(Long id);

    /**
     * 分页查询一级评论下的回复
     */
    Page<CommentVO> listReplies(Long parentId, int pageNum, int pageSize);
}