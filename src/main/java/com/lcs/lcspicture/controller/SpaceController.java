package com.lcs.lcspicture.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcs.lcspicture.annotation.AutoCheck;
import com.lcs.lcspicture.common.BaseResponse;
import com.lcs.lcspicture.common.DeleteRequest;
import com.lcs.lcspicture.common.ResultUtils;
import com.lcs.lcspicture.constant.UserConstant;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.exception.ThrowUtils;
import com.lcs.lcspicture.model.dto.space.*;
import com.lcs.lcspicture.model.entity.Space;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.enums.SpaceLeveEnum;
import com.lcs.lcspicture.model.vo.SpaceVO;
import com.lcs.lcspicture.service.SpaceService;
import com.lcs.lcspicture.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
public class SpaceController {
    @Resource
    private SpaceService spaceService;
    @Resource
    private UserService userService;

    /**
     * 创建空间
     *
     * @param spaceAddRequest 创建空间参数
     * @param request         请求
     * @return 空间id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long spaceId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(spaceId);
    }

    /**
     * 根据id删除空间
     *
     * @param deleteRequest id
     * @return 删除结果
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long deleteRequestId = deleteRequest.getId();
        // 判断空间是否存在
        Space oldSpace = spaceService.getById(deleteRequestId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅管理员或空间拥有者可删除
        if (!oldSpace.getUserId().equals(loginUser.getId()) || !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = spaceService.removeById(deleteRequestId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    /**
     * 更新空间
     *
     * @param spaceUpdateRequest 修改空间参数
     * @param request            请求
     * @return
     */
    @PostMapping("/update")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        //校验参数
        ThrowUtils.throwIf(spaceUpdateRequest == null || spaceUpdateRequest.getId() < 0, ErrorCode.PARAMS_ERROR);
        Space space = new Space();
        BeanUtil.copyProperties(spaceUpdateRequest, space);
        //填充审核参数
        spaceService.fillSpaceBySpaceLevel(space);
        //校验空间
        spaceService.validSpace(space, false);
        //判断空间是否存在
        Long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        //操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    /*
     * 获取空间列表(仅管理员可用)
     */
    @PostMapping("/list/page")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> getSpacePageList(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        Page<Space> page = spaceService.page(new Page<>(current, pageSize), spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(page);
    }

    /*
     * 获取空间列表(封装类)
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> getSpacePageListVO(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        //查询数据库
        Page<Space> page = spaceService.page(new Page<>(current, pageSize), spaceService.getQueryWrapper(spaceQueryRequest));
        //获取封装类
        return ResultUtils.success(spaceService.getSpaceVOList(page, request));
    }


    /*
    根据id获取空间(管理员)
     */
    @GetMapping("/get")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(@RequestParam Long id) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        //判断空间是否存在
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(space);
    }

    /*
    根据id获取空间(用户)
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(@RequestParam long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        //判断空间是否存在
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        //只有空间拥有者可以查看
        User loginUser = userService.getLoginUser(request);
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "空间未通过审核，无权查看");
        }
        // 空间拥有者或管理员可以查看未通过审核的空间
        return ResultUtils.success(spaceService.getSpaceVO(space, request));
    }

    /*
    修改空间（用户)
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        //校验参数
        ThrowUtils.throwIf(spaceEditRequest == null || spaceEditRequest.getId() < 0, ErrorCode.PARAMS_ERROR);
        //在此处讲实体类转和DTO进行转换
        Space space = new Space();
        BeanUtil.copyProperties(spaceEditRequest, space);
        //设置编辑时间
        space.setEditTime(new Date());
        //校验空间
        spaceService.validSpace(space, false);
        //判断空间是否存在
        Long id = spaceEditRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        User loginUser = userService.getLoginUser(request);
        //填充审核参数
        spaceService.fillSpaceBySpaceLevel(space);
        if (!loginUser.getId().equals(oldSpace.getUserId()) && !userService.isAdmin(loginUser)) {
            ThrowUtils.throwIf(true, ErrorCode.NO_AUTH_ERROR);
        }
        //操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    /**
     * 获取空间级别列表
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> getSpaceLevelList(){
        List<SpaceLevel> collect = Arrays.stream(SpaceLeveEnum.values())
                .map(spaceLeveEnum ->
                        new SpaceLevel(
                                spaceLeveEnum.getValue(),
                                spaceLeveEnum.getText(),
                                spaceLeveEnum.getMaxCount(),
                                spaceLeveEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(collect);
    }
}
