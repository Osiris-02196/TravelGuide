package com.oxiris.travelguide.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.oxiris.travelguide.annotation.AuthCheck;
import com.oxiris.travelguide.common.BaseResponse;
import com.oxiris.travelguide.common.DeleteRequest;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.common.ResultUtils;
import com.oxiris.travelguide.constant.UserConstant;
import com.oxiris.travelguide.exception.BusinessException;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.model.dto.user.*;
import com.oxiris.travelguide.model.vo.LoginUserVO;
import com.oxiris.travelguide.model.vo.UserVO;
import com.oxiris.travelguide.service.FollowService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.oxiris.travelguide.model.entity.User;
import com.oxiris.travelguide.service.UserService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户 控制层。
 *
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private FollowService followService;

    /**
     *用户注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("register")
    public BaseResponse<Long> userRigister(@RequestBody UserRegisterRequest userRegisterRequest){
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount,userPassword,checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录（获取用户脱敏信息）
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户(用于权限校验)
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户退出登录
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 获取任意用户的公开信息（个人主页使用，含关注数/粉丝数/是否已关注）
     */
    @GetMapping("/{id}/profile")
    public BaseResponse<UserVO> getUserProfile(@PathVariable Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        UserVO userVO = userService.getUserVO(user);
        // 填充关注信息
        Long currentUserId = null;
        try {
            User loginUser = userService.getLoginUser(request);
            if (loginUser != null) {
                currentUserId = loginUser.getId();
            }
        } catch (Exception ignored) {
            // 未登录时忽略
        }
        followService.fillFollowInfo(userVO, currentUserId);
        return ResultUtils.success(userVO);
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取要删除的用户
        User userToDelete = userService.getById(deleteRequest.getId());
        if (userToDelete == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        // 获取当前登录用户
        User currentUser = userService.getLoginUser(request);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "当前用户未登录");
        }
        // 检查当前用户是否具有删除管理员的权限
        if (UserConstant.ADMIN_ROLE.equals(userToDelete.getUserRole())
                && !UserConstant.SUPERADMIN_ROLE.equals(currentUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限删除管理员用户");
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        // 数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }


    /**
     * 修改用户状态（仅管理员）
     */
    @PostMapping("/update/status")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPERADMIN_ROLE})
    public BaseResponse<Boolean> updateUserStatus(@RequestBody UpdateUserStatusRequest updateUserStatusRequest,
                                                  HttpServletRequest request) {
        if (updateUserStatusRequest == null || updateUserStatusRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        // 状态值校验由 service 层完成
        Boolean result = userService.updateUserStatus(updateUserStatusRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 上传用户头像到COS
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        String url = userService.uploadAvatar(file);
        return ResultUtils.success(url);
    }

    /**
     * 修改当前登录用户个人资料（昵称、头像）
     */
    @PostMapping("/update/profile")
    public BaseResponse<Boolean> updateProfile(@RequestBody UserUpdateProfileRequest updateProfileRequest,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(updateProfileRequest == null, ErrorCode.PARAMS_ERROR);
        Boolean result = userService.updateUserProfile(updateProfileRequest, request);
        return ResultUtils.success(result);
    }

}
