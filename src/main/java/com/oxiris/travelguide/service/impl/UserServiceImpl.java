package com.oxiris.travelguide.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.constant.UserConstant;
import com.oxiris.travelguide.exception.BusinessException;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.manager.CosManager;
import com.oxiris.travelguide.model.dto.user.UpdateUserStatusRequest;
import com.oxiris.travelguide.model.dto.user.UserUpdatePasswordRequest;
import com.oxiris.travelguide.model.dto.user.UserQueryRequest;
import com.oxiris.travelguide.model.dto.user.UserUpdateProfileRequest;
import com.oxiris.travelguide.model.entity.User;
import com.oxiris.travelguide.mapper.UserMapper;
import com.oxiris.travelguide.model.enums.user.UserRoleEnum;
import com.oxiris.travelguide.model.enums.user.UserStatusEnum;
import com.oxiris.travelguide.model.vo.LoginUserVO;
import com.oxiris.travelguide.model.vo.UserVO;
import com.oxiris.travelguide.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.oxiris.travelguide.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{

    @Resource
    private CosManager cosManager;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查是否重复
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }


    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "oxiris";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        // 用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3.检查用户登录态
        Integer userStatus = user.getUserStatus();
        if (UserStatusEnum.BANNED.getValue().equals(userStatus)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被封禁，无法登录");
        }
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 5. 获得脱敏后的用户信息
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .eq("userAccount", userAccount)
                .like("userName", userName)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }


    @Override
    public Boolean updateUserStatus(UpdateUserStatusRequest updateUserStatusRequest, HttpServletRequest request) {
        Long userId = updateUserStatusRequest.getId();
        Integer status = updateUserStatusRequest.getUserStatus();

        // 获取目标用户
        User targetUser = this.getById(userId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 如果目标用户是管理员，则需要超级管理员才能修改其状态
        if (UserConstant.ADMIN_ROLE.equals(targetUser.getUserRole())) {
            User currentUser = this.getLoginUser(request);
            if (!UserConstant.SUPERADMIN_ROLE.equals(currentUser.getUserRole())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有超级管理员才能修改管理员的状态");
            }
        }

        // 更新状态
        targetUser.setUserStatus(status);
        boolean result = this.updateById(targetUser);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改用户状态失败");
        }
        return true;
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        // 1. 校验文件
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        // 2. 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String key = "avatar/" + UUID.randomUUID().toString().replace("-", "") + suffix;
        // 3. 将MultipartFile转为临时File
        File tempFile = null;
        try {
            tempFile = File.createTempFile("avatar_", suffix);
            file.transferTo(tempFile);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件处理失败");
        }
        // 4. 上传到COS
        String url = cosManager.uploadFile(key, tempFile);
        // 5. 删除临时文件
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
        ThrowUtils.throwIf(url == null, ErrorCode.SYSTEM_ERROR, "头像上传失败");
        return url;
    }

    @Override
    public Boolean updateUserProfile(UserUpdateProfileRequest updateProfileRequest, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(updateProfileRequest == null, ErrorCode.PARAMS_ERROR);
        String userName = updateProfileRequest.getUserName();
        String userAvatar = updateProfileRequest.getUserAvatar();
        if (StrUtil.isBlank(userName) && StrUtil.isBlank(userAvatar)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称和头像不能同时为空");
        }
        // 2. 获取当前登录用户
        User loginUser = getLoginUser(request);
        // 3. 更新昵称
        if (StrUtil.isNotBlank(userName)) {
            loginUser.setUserName(userName);
        }
        // 4. 更新头像
        if (StrUtil.isNotBlank(userAvatar)) {
            loginUser.setUserAvatar(userAvatar);
        }
        // 5. 保存到数据库
        boolean result = this.updateById(loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "修改个人资料失败");
        return true;
    }

    @Override
    public Boolean updatePassword(UserUpdatePasswordRequest updatePasswordRequest, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(updatePasswordRequest == null, ErrorCode.PARAMS_ERROR);
        String oldPassword = updatePasswordRequest.getOldPassword();
        String newPassword = updatePasswordRequest.getNewPassword();
        String checkPassword = updatePasswordRequest.getCheckPassword();
        if (StrUtil.hasBlank(oldPassword, newPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度不能小于8位");
        }
        if (!newPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        }
        // 2. 获取当前登录用户
        User loginUser = getLoginUser(request);
        // 3. 校验旧密码
        String encryptOldPassword = getEncryptPassword(oldPassword);
        if (!encryptOldPassword.equals(loginUser.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码错误");
        }
        // 4. 新旧密码不能相同
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能与旧密码相同");
        }
        // 5. 加密新密码并保存
        String encryptNewPassword = getEncryptPassword(newPassword);
        loginUser.setUserPassword(encryptNewPassword);
        boolean result = this.updateById(loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "修改密码失败");
        return true;
    }

}
