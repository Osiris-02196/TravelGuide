package com.oxiris.travelguide.controller;

import com.mybatisflex.core.paginate.Page;
import com.oxiris.travelguide.annotation.AuthCheck;
import com.oxiris.travelguide.annotation.StatusCheck;
import com.oxiris.travelguide.common.BaseResponse;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.common.ResultUtils;
import com.oxiris.travelguide.constant.UserConstant;
import com.oxiris.travelguide.exception.ThrowUtils;

import com.oxiris.travelguide.model.dto.strategy.StrategyAddRequest;
import com.oxiris.travelguide.model.dto.strategy.StrategyQueryRequest;
import com.oxiris.travelguide.model.vo.StrategyCollectVO;
import com.oxiris.travelguide.model.vo.StrategyVO;
import com.oxiris.travelguide.service.StrategyService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 攻略 控制层。
 *
 */
@RestController
@RequestMapping("/strategy")
public class StrategyController {

    @Resource
    private StrategyService strategyService;

    /**
     * 上传攻略图片到COS
     *
     * @param file 图片文件
     * @return 图片URL
     */
    @PostMapping("/upload/image")
    public BaseResponse<String> uploadImage(@RequestParam("file") MultipartFile file) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        String url = strategyService.uploadImage(file);
        return ResultUtils.success(url);
    }

    /**
     * 用户上传攻略
     *
     * @param strategyAddRequest 攻略上传请求
     * @param request            HTTP请求
     * @return 攻略ID
     */
    @PostMapping("/add")
    @StatusCheck(allowedStatus = UserConstant.NORMAL) // 只有正常状态的用户可以上传攻略
    public BaseResponse<Long> addStrategy(@RequestBody StrategyAddRequest strategyAddRequest,
                                          HttpServletRequest request) {
        ThrowUtils.throwIf(strategyAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = strategyService.addStrategy(strategyAddRequest, request);
        return ResultUtils.success(id);
    }

    /**
     * 获取攻略详情（点击量+1）
     *
     * @param id 攻略ID
     * @return 攻略VO
     */
    @GetMapping("/detail/{id}")
    public BaseResponse<StrategyVO> getStrategyDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        StrategyVO strategyVO = strategyService.getStrategyDetail(id);
        return ResultUtils.success(strategyVO);
    }

    /**
     * 分页查询主页审核通过的最新攻略列表（时间排序，所有用户可见）
     *
     * @param strategyQueryRequest 查询请求
     * @return 分页攻略VO
     */
    @PostMapping("/list/passed")
    @StatusCheck(allowedStatus = {UserConstant.NORMAL, UserConstant.MUTED}) //允许正常和禁言的状态的用户访问
    public BaseResponse<Page<StrategyVO>> listPassedStrategies(@RequestBody StrategyQueryRequest strategyQueryRequest) {
        ThrowUtils.throwIf(strategyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<StrategyVO> result = strategyService.listPassedStrategies(strategyQueryRequest);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询审核通过的热门推荐列表（热度排序，所有用户可见）
     *
     * @param strategyQueryRequest 查询请求
     * @return 分页攻略VO
     */
    @PostMapping("/list/hot")
    public BaseResponse<Page<StrategyVO>> listHotStrategies(@RequestBody StrategyQueryRequest strategyQueryRequest) {
        ThrowUtils.throwIf(strategyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<StrategyVO> result = strategyService.listHotStrategies(strategyQueryRequest);
        return ResultUtils.success(result);
    }

    /**
     * 用户分页查询自己上传的攻略（按状态和时间排序）
     * 通过strategyStatus字段筛选：不传则查所有，0-待审核 1-通过 2-拒绝
     *
     * @param strategyQueryRequest 查询请求
     * @param request              HTTP请求
     * @return 分页攻略VO
     */
    @PostMapping("/list/my")
    public BaseResponse<Page<StrategyVO>> listMyStrategies(@RequestBody StrategyQueryRequest strategyQueryRequest,
                                                           HttpServletRequest request) {
        ThrowUtils.throwIf(strategyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<StrategyVO> result = strategyService.listMyStrategies(strategyQueryRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 用户删除自己上传通过的攻略（逻辑删除）
     *
     * @param id      攻略ID
     * @param request HTTP请求
     * @return 是否成功
     */
    @PostMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteStrategy(@PathVariable Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Boolean result = strategyService.deleteStrategy(id, request);
        return ResultUtils.success(result);
    }

    /**
     * 按市级地点分页查询审核通过的攻略（官方推荐置顶）
     *
     * @param strategyQueryRequest 查询请求（需包含location字段）
     * @return 分页攻略VO
     */
    @PostMapping("/list/by-location")
    public BaseResponse<Page<StrategyVO>> listStrategiesByLocation(@RequestBody StrategyQueryRequest strategyQueryRequest) {
        ThrowUtils.throwIf(strategyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<StrategyVO> result = strategyService.listStrategiesByLocation(strategyQueryRequest);
        return ResultUtils.success(result);
    }

    /**
     * 点赞攻略
     *
     * @param id 攻略ID
     * @return 是否成功
     */
    @PostMapping("/like/{id}")
    public BaseResponse<Boolean> likeStrategy(@PathVariable Long id,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Boolean result = strategyService.likeStrategy(id, request);
        return ResultUtils.success(result);
    }

    /**
     * 收藏攻略
     *
     * @param id      攻略ID
     * @param request HTTP请求
     * @return 是否成功
     */
    @PostMapping("/collect/{id}")
    public BaseResponse<Boolean> collectStrategy(@PathVariable Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Boolean result = strategyService.collectStrategy(id, request);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询当前用户的收藏列表（按收藏时间倒序）
     *
     * @param strategyQueryRequest 查询请求（仅使用分页参数）
     * @param request              HTTP请求
     * @return 分页收藏VO
     */
    @PostMapping("/list/my-collect")
    public BaseResponse<Page<StrategyCollectVO>> listUserCollectStrategies(
            @RequestBody StrategyQueryRequest strategyQueryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(strategyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<StrategyCollectVO> result = strategyService.listUserCollectStrategies(strategyQueryRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询某个用户的审核通过攻略列表（公开）
     *
     * @param userId               用户ID
     * @param strategyQueryRequest 查询请求
     * @return 分页攻略VO
     */
    @GetMapping("/list/user/{userId}")
    public BaseResponse<Page<StrategyVO>> listUserStrategies(@PathVariable Long userId,
                                                              StrategyQueryRequest strategyQueryRequest) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        Page<StrategyVO> result = strategyService.listUserStrategies(userId, strategyQueryRequest);
        return ResultUtils.success(result);
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员分页查询待审核的攻略列表
     *
     * @param strategyQueryRequest 查询请求
     * @return 分页攻略VO
     */
    @PostMapping("/admin/list/pending")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Page<StrategyVO>> listPendingStrategies(@RequestBody StrategyQueryRequest strategyQueryRequest) {
        ThrowUtils.throwIf(strategyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<StrategyVO> result = strategyService.listPendingStrategies(strategyQueryRequest);
        return ResultUtils.success(result);
    }

    /**
     * 管理员修改攻略审核状态
     *
     * @param id     攻略ID
     * @param status 审核状态（1-通过，2-拒绝）
     * @return 是否成功
     */
    @PutMapping("/admin/audit/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Boolean> auditStrategy(@PathVariable Long id, @RequestParam Integer status) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Boolean result = strategyService.auditStrategy(id, status);
        return ResultUtils.success(result);
    }

    /**
     * 管理员设置/取消官方推荐
     *
     * @param id         攻略ID
     * @param isOfficial 是否官方推荐（0-否 1-是）
     * @return 是否成功
     */
    @PutMapping("/admin/official/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Boolean> setOfficial(@PathVariable Long id, @RequestParam Integer isOfficial) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Boolean result = strategyService.setOfficial(id, isOfficial);
        return ResultUtils.success(result);
    }

    /**
     * 管理员删除任意攻略（逻辑删除，无需归属校验）
     */
    @PostMapping("/admin/delete/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Boolean> adminDeleteStrategy(@PathVariable Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Boolean result = strategyService.adminDeleteStrategy(id, request);
        return ResultUtils.success(result);
    }
}
