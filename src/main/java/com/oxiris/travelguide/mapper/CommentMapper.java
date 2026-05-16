package com.oxiris.travelguide.mapper;

import com.mybatisflex.core.BaseMapper;
import com.oxiris.travelguide.model.entity.Comment;
import org.apache.ibatis.annotations.Select;

/**
 * 评论表 映射层。
 *
 */
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 根据ID查询评论（包含逻辑删除的）
     */
    @Select("SELECT * FROM comment WHERE id = #{id}")
    Comment selectByIdIncludeDeleted(Long id);
}
