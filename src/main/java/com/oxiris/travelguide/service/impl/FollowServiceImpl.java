package com.oxiris.travelguide.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.mapper.UserFollowMapper;
import com.oxiris.travelguide.model.entity.User;
import com.oxiris.travelguide.model.entity.UserFollow;
import com.oxiris.travelguide.model.vo.UserVO;
import com.oxiris.travelguide.service.FollowService;
import com.oxiris.travelguide.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl implements FollowService {

    @Resource
    private UserFollowMapper userFollowMapper;

    @Resource
    private UserService userService;

    @Override
    @Transactional
    public Boolean toggleFollow(Long followedUserId, HttpServletRequest request) {
        ThrowUtils.throwIf(followedUserId == null || followedUserId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser.getId().equals(followedUserId), ErrorCode.PARAMS_ERROR, "不能关注自己");
        User targetUser = userService.getById(followedUserId);
        ThrowUtils.throwIf(targetUser == null, ErrorCode.NOT_FOUND_ERROR, "目标用户不存在");

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("followerId", loginUser.getId())
                .eq("followedUserId", followedUserId);
        UserFollow existing = userFollowMapper.selectOneByQuery(queryWrapper);
        if (existing != null) {
            userFollowMapper.deleteById(existing.getId());
            return false;
        } else {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(loginUser.getId());
            follow.setFollowedUserId(followedUserId);
            userFollowMapper.insert(follow, true);
            return true;
        }
    }

    @Override
    public Boolean isFollowed(Long followedUserId, HttpServletRequest request) {
        ThrowUtils.throwIf(followedUserId == null || followedUserId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("followerId", loginUser.getId())
                .eq("followedUserId", followedUserId);
        return userFollowMapper.selectCountByQuery(queryWrapper) > 0;
    }

    @Override
    public Page<UserVO> listFollowing(Long userId, int pageNum, int pageSize) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        Page<UserFollow> followPage = userFollowMapper.paginate(
                Page.of(pageNum, pageSize),
                QueryWrapper.create()
                        .eq("followerId", userId)
                        .orderBy("createTime", false)
        );
        List<UserFollow> followList = followPage.getRecords();
        Page<UserVO> voPage = new Page<>(pageNum, pageSize, followPage.getTotalRow());
        if (CollUtil.isNotEmpty(followList)) {
            List<Long> userIds = followList.stream()
                    .map(UserFollow::getFollowedUserId)
                    .collect(Collectors.toList());
            List<User> users = userService.listByIds(userIds);
            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            List<UserVO> voList = followList.stream().map(f -> {
                User user = userMap.get(f.getFollowedUserId());
                if (user != null) {
                    UserVO vo = userService.getUserVO(user);
                    vo.setCreateTime(f.getCreateTime());
                    return vo;
                }
                return null;
            }).filter(vo -> vo != null).collect(Collectors.toList());
            voPage.setRecords(voList);
        } else {
            voPage.setRecords(new ArrayList<>());
        }
        return voPage;
    }

    @Override
    public Page<UserVO> listFollowers(Long userId, int pageNum, int pageSize) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        Page<UserFollow> followPage = userFollowMapper.paginate(
                Page.of(pageNum, pageSize),
                QueryWrapper.create()
                        .eq("followedUserId", userId)
                        .orderBy("createTime", false)
        );
        List<UserFollow> followList = followPage.getRecords();
        Page<UserVO> voPage = new Page<>(pageNum, pageSize, followPage.getTotalRow());
        if (CollUtil.isNotEmpty(followList)) {
            List<Long> userIds = followList.stream()
                    .map(UserFollow::getFollowerId)
                    .collect(Collectors.toList());
            List<User> users = userService.listByIds(userIds);
            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            List<UserVO> voList = followList.stream().map(f -> {
                User user = userMap.get(f.getFollowerId());
                if (user != null) {
                    UserVO vo = userService.getUserVO(user);
                    return vo;
                }
                return null;
            }).filter(vo -> vo != null).collect(Collectors.toList());
            voPage.setRecords(voList);
        } else {
            voPage.setRecords(new ArrayList<>());
        }
        return voPage;
    }

    @Override
    public long[] getFollowCounts(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        long followCount = userFollowMapper.selectCountByQuery(
                QueryWrapper.create().eq("followerId", userId));
        long followerCount = userFollowMapper.selectCountByQuery(
                QueryWrapper.create().eq("followedUserId", userId));
        return new long[]{followCount, followerCount};
    }

    @Override
    public void fillFollowInfo(UserVO userVO, Long currentUserId) {
        if (userVO == null || userVO.getId() == null) return;
        long[] counts = getFollowCounts(userVO.getId());
        userVO.setFollowCount(counts[0]);
        userVO.setFollowerCount(counts[1]);
        if (currentUserId != null) {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq("followerId", currentUserId)
                    .eq("followedUserId", userVO.getId());
            userVO.setIsFollowed(userFollowMapper.selectCountByQuery(queryWrapper) > 0);
        }
    }
}
