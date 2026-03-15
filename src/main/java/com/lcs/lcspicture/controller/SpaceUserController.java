package com.lcs.lcspicture.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.lcs.lcspicture.common.BaseResponse;
import com.lcs.lcspicture.common.DeleteRequest;
import com.lcs.lcspicture.common.ResultUtils;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.exception.ThrowUtils;
import com.lcs.lcspicture.manager.auth.annotation.SaSpaceCheckPermission;
import com.lcs.lcspicture.manager.auth.model.SpaceUserPermissionConstant;
import com.lcs.lcspicture.model.dto.spaceuser.SpaceUserAddRequest;
import com.lcs.lcspicture.model.dto.spaceuser.SpaceUserEditRequest;
import com.lcs.lcspicture.model.dto.spaceuser.SpaceUserQueryRequest;
import com.lcs.lcspicture.model.entity.SpaceUser;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.vo.SpaceUserVO;
import com.lcs.lcspicture.service.SpaceUserService;
import com.lcs.lcspicture.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private UserService userService;

    /**
     * 添加成员到空间
     *
     * @param spaceUserAddRequest 创建空间参数
     * @param request             请求
     * @return 空间id
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(spaceId);
    }

    /**
     * 从空间移除成员
     *
     * @param deleteRequest id
     * @return 删除结果
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long deleteRequestId = deleteRequest.getId();
        // 判断空间是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(deleteRequestId);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = spaceUserService.removeById(deleteRequestId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    /*
     * 获取空间成员列表(仅管理员可用)
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> SpaceUserList(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        //校验参数
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService
                .list(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 查询某个成员在某个空间的信息
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @GetMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        //校验参数
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        //判断空间是否存在
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }


    /*
    编辑成员信息（用户)
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceUserEditRequest spaceUserEditRequest, HttpServletRequest request) {
        //校验参数
        ThrowUtils.throwIf(spaceUserEditRequest == null || spaceUserEditRequest.getId() < 0, ErrorCode.PARAMS_ERROR);
        //在此处讲实体类转和DTO进行转换
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserEditRequest, spaceUser);
        //校验空间
        spaceUserService.validSpaceUser(spaceUser, false);
        //判断空间是否存在
        Long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        //操作数据库
        boolean result = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    /**
     * 查询我加入的空间列表
     *
     * @param request
     * @return
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> list = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(list));
    }
}
