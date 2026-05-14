package com.oxiris.travelguide.service;

import com.mybatisflex.core.paginate.Page;
import com.oxiris.travelguide.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

public interface FollowService {

    /**
     * 关注/取消关注（toggle）
     *
     * @param followedUserId 被关注者ID
     * @param request        HTTP请求
     * @return true-已关注，false-已取消
     */
    Boolean toggleFollow(Long followedUserId, HttpServletRequest request);

    /**
     * 检查当前用户是否已关注目标用户
     *
     * @param followedUserId 目标用户ID
     * @param request        HTTP请求
     * @return true-已关注
     */
    Boolean isFollowed(Long followedUserId, HttpServletRequest request);

    /**
     * 获取关注列表（分页）
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页用户VO
     */
    Page<UserVO> listFollowing(Long userId, int pageNum, int pageSize);

    /**
     * 获取粉丝列表（分页）
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页用户VO
     */
    Page<UserVO> listFollowers(Long userId, int pageNum, int pageSize);

    /**
     * 获取关注数和粉丝数
     *
     * @param userId 用户ID
     * @return [followCount, followerCount]
     */
    long[] getFollowCounts(Long userId);

    /**
     * 填充用户VO的关注信息
     *
     * @param userVO     目标用户VO
     * @param currentUserId 当前登录用户ID（可为null）
     */
    void fillFollowInfo(UserVO userVO, Long currentUserId);
}
