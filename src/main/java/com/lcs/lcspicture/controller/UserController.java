package com.lcs.lcspicture.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcs.lcspicture.annotation.AutoCheck;
import com.lcs.lcspicture.common.BaseResponse;
import com.lcs.lcspicture.common.DeleteRequest;
import com.lcs.lcspicture.common.PageRequest;
import com.lcs.lcspicture.common.ResultUtils;
import com.lcs.lcspicture.constant.UserConstant;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.exception.ThrowUtils;
import com.lcs.lcspicture.model.dto.user.UserAddRequest;
import com.lcs.lcspicture.model.dto.user.UserLoginRequest;
import com.lcs.lcspicture.model.dto.user.UserQueryRequest;
import com.lcs.lcspicture.model.dto.user.UserRegisterRequest;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.vo.LoginUserVO;
import com.lcs.lcspicture.model.vo.UserVO;
import com.lcs.lcspicture.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    /*
     *用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /*
     * 用户注册
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /*
     * 获取当前登录用户信息
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /*
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /*
     *创建用户
     */
    @PostMapping("/add")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> userRegister(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        //设置默认密码
        final String DEFAULT_PASSWORD = "123456";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        //插入数据库
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /*
     *根据id删除用户
     */
    @PostMapping("/delete")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> userDelete(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /*
     *根据id更新用户
     */
    @PostMapping("/update")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> userUpdate(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }


    /*
    分页获取用户列表（需要脱敏）
     */
    @PostMapping("/list/page/vo")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = userQueryRequest.getCurrent();
        int size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /*
    根据id获取用户(仅管理员)
     */
    @GetMapping("/get")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }
    /*
    根据id获取用户包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id) {
        BaseResponse<User> user = this.getUserById(id);
        User data = user.getData();
        return ResultUtils.success(userService.getUserVO(data));
    }
}
