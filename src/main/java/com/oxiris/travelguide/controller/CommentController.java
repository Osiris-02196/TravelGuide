package com.oxiris.travelguide.controller;

import com.mybatisflex.core.paginate.Page;
import com.oxiris.travelguide.common.BaseResponse;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.common.ResultUtils;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.model.dto.comment.CommentAddRequest;
import com.oxiris.travelguide.model.dto.comment.CommentQueryRequest;
import com.oxiris.travelguide.model.vo.CommentVO;
import com.oxiris.travelguide.service.CommentService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 评论表 控制层。
 */
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Resource
    private CommentService commentService;

    /**
     * 添加评论
     *
     * @param commentAddRequest 评论添加请求
     * @param request           HTTP请求
     * @return 评论ID
     */
    @PostMapping("/add")
    public BaseResponse<Long> addComment(@RequestBody CommentAddRequest commentAddRequest,
                                         HttpServletRequest request) {
        ThrowUtils.throwIf(commentAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = commentService.addComment(commentAddRequest, request);
        return ResultUtils.success(id);
    }

    /**
     * 分页查询评论列表
     *
     * @param commentQueryRequest 查询请求
     * @return 分页评论VO
     */
    @PostMapping("/list")
    public BaseResponse<Page<CommentVO>> listComments(@RequestBody CommentQueryRequest commentQueryRequest) {
        ThrowUtils.throwIf(commentQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<CommentVO> result = commentService.listComments(commentQueryRequest);
        return ResultUtils.success(result);
    }

    /**
     * 点赞评论
     *
     * @param id 评论ID
     * @return 是否成功
     */
    @PostMapping("/like/{id}")
    public BaseResponse<Boolean> likeComment(@PathVariable Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Boolean result = commentService.likeComment(id, request);
        return ResultUtils.success(result);
    }
}
