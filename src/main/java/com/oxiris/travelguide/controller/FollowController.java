package com.oxiris.travelguide.controller;

import com.mybatisflex.core.paginate.Page;
import com.oxiris.travelguide.common.BaseResponse;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.common.ResultUtils;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.model.vo.UserVO;
import com.oxiris.travelguide.service.FollowService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private FollowService followService;

    /**
     * 关注/取消关注（toggle）
     */
    @PostMapping("/{followedUserId}")
    public BaseResponse<Map<String, Object>> toggleFollow(@PathVariable Long followedUserId,
                                                           HttpServletRequest request) {
        ThrowUtils.throwIf(followedUserId == null || followedUserId <= 0, ErrorCode.PARAMS_ERROR);
        Boolean followed = followService.toggleFollow(followedUserId, request);
        Map<String, Object> result = new HashMap<>();
        result.put("followed", followed);
        return ResultUtils.success(result);
    }

    /**
     * 检查当前用户是否已关注目标用户
     */
    @GetMapping("/check/{followedUserId}")
    public BaseResponse<Boolean> isFollowed(@PathVariable Long followedUserId,
                                            HttpServletRequest request) {
        ThrowUtils.throwIf(followedUserId == null || followedUserId <= 0, ErrorCode.PARAMS_ERROR);
        Boolean result = followService.isFollowed(followedUserId, request);
        return ResultUtils.success(result);
    }

    /**
     * 获取关注列表（分页）
     */
    @GetMapping("/following/{userId}")
    public BaseResponse<Page<UserVO>> listFollowing(@PathVariable Long userId,
                                                     @RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "20") int pageSize) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        Page<UserVO> result = followService.listFollowing(userId, pageNum, pageSize);
        return ResultUtils.success(result);
    }

    /**
     * 获取粉丝列表（分页）
     */
    @GetMapping("/followers/{userId}")
    public BaseResponse<Page<UserVO>> listFollowers(@PathVariable Long userId,
                                                     @RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "20") int pageSize) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        Page<UserVO> result = followService.listFollowers(userId, pageNum, pageSize);
        return ResultUtils.success(result);
    }

    /**
     * 获取关注数和粉丝数
     */
    @GetMapping("/count/{userId}")
    public BaseResponse<Map<String, Long>> getFollowCounts(@PathVariable Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        long[] counts = followService.getFollowCounts(userId);
        Map<String, Long> result = new HashMap<>();
        result.put("followCount", counts[0]);
        result.put("followerCount", counts[1]);
        return ResultUtils.success(result);
    }
}
