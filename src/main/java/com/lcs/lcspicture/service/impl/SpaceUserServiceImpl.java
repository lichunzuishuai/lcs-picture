package com.lcs.lcspicture.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.exception.ThrowUtils;
import com.lcs.lcspicture.model.dto.spaceuser.SpaceUserAddRequest;
import com.lcs.lcspicture.model.dto.spaceuser.SpaceUserQueryRequest;
import com.lcs.lcspicture.model.entity.Space;
import com.lcs.lcspicture.model.entity.SpaceUser;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.enums.SpaceRoleEnum;
import com.lcs.lcspicture.model.vo.SpaceUserVO;
import com.lcs.lcspicture.model.vo.SpaceVO;
import com.lcs.lcspicture.model.vo.UserVO;
import com.lcs.lcspicture.service.SpaceService;
import com.lcs.lcspicture.service.SpaceUserService;
import com.lcs.lcspicture.mapper.SpaceUserMapper;
import com.lcs.lcspicture.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author lcs
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2026-02-27 00:30:55
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    @Lazy
    private SpaceService spaceService;
    @Resource
    private UserService userService;

    /**
     * 创建空间成员
     */
    @Override
    public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        //参数校验
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR, "参数为空");
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserAddRequest, spaceUser);
        //校验参数
        validSpaceUser(spaceUser, true);
        //操作数据库
        boolean save = this.save(spaceUser);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "创建空间成员失败");
        return spaceUser.getId();
    }

    /**
     * 校验空间成员
     *
     * @param spaceUser 空间成员添加请求
     * @param add       是否为创建
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, Boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR, "参数为空");
        // 添加空间成员时，参数不能为空，且空间和用户都存在
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在");
            // 创建时，不能重复创建
            boolean exists = this.lambdaQuery()
                    .eq(SpaceUser::getUserId, userId)
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .exists();
            ThrowUtils.throwIf(exists, ErrorCode.PARAMS_ERROR, "空间成员已存在");
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleValue = SpaceRoleEnum.getSpaceRoleValue(spaceRole);
        if (spaceRole != null && spaceRoleValue == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色错误");
        }
        // 删除成员时，参数不能为空，且空间和用户都存在
       /* boolean exists = this.lambdaQuery()
                .eq(SpaceUser::getUserId, userId)
                .eq(SpaceUser::getSpaceId, spaceId)
                .exists();
        ThrowUtils.throwIf(!exists, ErrorCode.PARAMS_ERROR, "空间成员不存在");*/
    }

    /**
     * 获取单个空间成员包装类
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        //对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        //关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
        //关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space);
            spaceUserVO.setSpace(spaceVO);
        }
        //返回封装类
        return spaceUserVO;
    }

    /**
     * 获取空间成员列表包装类
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        if (spaceUserList == null || spaceUserList.isEmpty()) {
            return List.of();
        }
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream()
                .map(spaceUser -> getSpaceUserVO(spaceUser, null)).toList();
        // 关联查询用户信息
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        // 批量查询用户信息（空集合时不调用 listByIds，避免生成 IN () 导致 SQL 语法错误）
        Map<Long, List<User>> longUserListMap = userIdSet.isEmpty()
                ? Map.of()
                : userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> longSpaceListMap = spaceIdSet.isEmpty()
                ? Map.of()
                : spaceService.listByIds(spaceIdSet).stream().collect(Collectors.groupingBy(Space::getId));
        // 遍历空间成员列表，设置用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            // 设置用户信息
            User user = null;
            if (longUserListMap.containsKey(userId)) {
                user = longUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVO(user));
            // 设置空间信息
            Space space = null;
            if (longSpaceListMap.containsKey(spaceId)) {
                space = longSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(spaceService.getSpaceVO(space));
        });
        return spaceUserVOList;
    }

    /**
     * 获取空间成员查询条件
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 取出参数
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id)
                .eq(ObjUtil.isNotNull(spaceId), "spaceId", spaceId)
                .eq(ObjUtil.isNotNull(userId), "userId", userId)
                .eq(StrUtil.isNotBlank(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }
}




