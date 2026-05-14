package com.oxiris.travelguide.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.oxiris.travelguide.model.dto.user.UpdateUserStatusRequest;
import com.oxiris.travelguide.model.dto.user.UserUpdateProfileRequest;
import com.oxiris.travelguide.model.dto.user.UserQueryRequest;
import com.oxiris.travelguide.model.entity.User;
import com.oxiris.travelguide.model.vo.LoginUserVO;
import com.oxiris.travelguide.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户 服务层。
 *
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 密码加密
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户退出登录
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取单个用户信息
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     *  获取用户信息列表
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     *  将查询请求转为QueryWrapper
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 修改用户状态（仅管理员）
     *
     * @param updateUserStatusRequest 请求参数
     * @param request                 当前请求（用于获取登录用户）
     * @return 是否成功
     */
    Boolean updateUserStatus(UpdateUserStatusRequest updateUserStatusRequest, HttpServletRequest request);

    /**
     * 上传用户头像到COS
     *
     * @param file 图片文件
     * @return 头像访问URL
     */
    String uploadAvatar(MultipartFile file);

    /**
     * 修改当前登录用户的个人资料（昵称、头像）
     *
     * @param updateProfileRequest 修改资料请求
     * @param request              HTTP请求
     * @return 是否成功
     */
    Boolean updateUserProfile(UserUpdateProfileRequest updateProfileRequest, HttpServletRequest request);
}
