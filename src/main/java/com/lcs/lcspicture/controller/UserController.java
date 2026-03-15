package com.lcs.lcspicture.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcs.lcspicture.annotation.AutoCheck;
import com.lcs.lcspicture.common.BaseResponse;
import com.lcs.lcspicture.common.DeleteRequest;
import com.lcs.lcspicture.common.ResultUtils;
import com.lcs.lcspicture.config.CosClientConfig;
import com.lcs.lcspicture.constant.UserConstant;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.exception.ThrowUtils;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.model.dto.user.UserAddRequest;
import com.lcs.lcspicture.model.dto.user.UserLoginRequest;
import com.lcs.lcspicture.model.dto.user.UserQueryRequest;
import com.lcs.lcspicture.model.dto.user.UserRegisterRequest;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.vo.LoginUserVO;
import com.lcs.lcspicture.model.vo.UserVO;
import com.lcs.lcspicture.service.UserService;
import com.lcs.lcspicture.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import cn.hutool.core.util.RandomUtil;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;
    @Resource
    private CosClientConfig cosClientConfig;

    /*
     * 用户注册
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
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest,
                                               HttpServletRequest request) {
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
     * 创建用户
     */
    @PostMapping("/add")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> userRegister(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 设置默认密码
        final String DEFAULT_PASSWORD = "123456";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        // 插入数据库
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /*
     * 根据id删除用户
     */
    @PostMapping("/delete")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> userDelete(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /*
     * 根据id更新用户
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
     * 分页获取用户列表（需要脱敏）
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
     * 根据id获取用户(仅管理员)
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
     * 根据id获取用户包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id) {
        BaseResponse<User> user = this.getUserById(id);
        User data = user.getData();
        return ResultUtils.success(userService.getUserVO(data));
    }

    /**
     * 更新用户头像
     */
    @PostMapping("/update/avatar")
    public BaseResponse<String> updateAvatar(@RequestPart("file") MultipartFile multipartFile,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(multipartFile == null || multipartFile.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        LoginUserVO loginUser = userService.getLoginUserVO(userService.getLoginUser(request));
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 构建存入COS的路径
        String originalFilename = multipartFile.getOriginalFilename();
        // 获取文件后缀
        String suffix = originalFilename != null ? FileUtil.getSuffix(originalFilename) : ".jpg";
        String fileName = RandomUtil.randomString(16) + suffix;
        String filePath = String.format("public/user_avatar/%s/%s", loginUser.getId(), fileName);

        File file = null;
        try {
            // 在本地创建临时文件
            file = File.createTempFile(filePath.replace("/", ""), null);
            // 将上传的文件保存到临时文件中
            multipartFile.transferTo(file);
            // 上传到对象存储
            cosManager.putObject(filePath, file);

            // 获取可访问的图片URL (假设这里只是返回路径，具体URL拼接需要根据你的COS配置，可能是直接的filePath或者完整的http链接)
            // 这里为了通用，很多时候COS配置直接配置域名映射，或者通过特定的Host组装，比如
            // "https://你的桶名.cos.ap-guangzhou.myqcloud.com/" + filePath
            // 因为不知道你具体的COS配置拼接方式，这里先拼接一个通用的存入数据库的路径 (如果你的前端或者其他地方会自动补全前缀，直接存filePath即可)
            // 简单处理，假如数据库存 COS 的相对路径
            String avatarUrl = cosClientConfig.getHost() + "/" + filePath; // 你可以根据自己的实际情况加上 "http://xxx/" 头

            // 更新数据库
            User userUpdate = new User();
            userUpdate.setUserAvatar(avatarUrl);
            userUpdate.setId(loginUser.getId());
            boolean result = userService.updateById(userUpdate);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "头像更新失败");
            User user = userService.getById(loginUser.getId());
            request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE,user);
            return ResultUtils.success(avatarUrl);
        } catch (Exception e) {
            log.error("Avatar file upload error, filePath={}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        } finally {
            if (file != null) {
                boolean delete = file.delete();
                if (!delete) {
                    log.error("Avatar file delete error, filePath={}", file.getAbsolutePath());
                }
            }
        }
    }
}
