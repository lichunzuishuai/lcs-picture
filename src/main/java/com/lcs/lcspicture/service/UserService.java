package com.lcs.lcspicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lcs.lcspicture.model.dto.user.UserQueryRequest;
import com.lcs.lcspicture.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcs.lcspicture.model.vo.LoginUserVO;
import com.lcs.lcspicture.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author lcs
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2026-01-17 20:53:19
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @return 返回脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取脱敏后的用户登录信息
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息
     *
     * @param userList 用户列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 密码加密
     *
     * @param userPassword 用户密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取当前登录用户
     *
     * @param request 请求
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     */
    boolean userLogout(HttpServletRequest request);

    /*
     * 获取用户查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 判断是否是管理员
     *
     * @param user 用户
     * @return 是否是管理员
     */
    boolean isAdmin(User user);
}
