package com.lcs.lcspicture.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.exception.ThrowUtils;
import com.lcs.lcspicture.mapper.SpaceMapper;
import com.lcs.lcspicture.model.dto.space.SpaceAddRequest;
import com.lcs.lcspicture.model.dto.space.SpaceQueryRequest;
import com.lcs.lcspicture.model.entity.Space;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.enums.SpaceLeveEnum;
import com.lcs.lcspicture.model.vo.SpaceVO;
import com.lcs.lcspicture.model.vo.UserVO;
import com.lcs.lcspicture.service.SpaceService;
import com.lcs.lcspicture.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author lcs
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2026-01-27 09:10:10
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;
    @Resource
    private TransactionTemplate transactionTemplate;


    /**
     * 校验
     *
     * @param space 空间
     * @param add   是否为创建
     */
    @Override
    public void validSpace(Space space, Boolean add) {
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLeveEnum spaceLeveEnum = SpaceLeveEnum.getSpaceValue(spaceLevel);
        // 创建时
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
        }
        // 更新时
        if (spaceLevel != null && spaceLeveEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别错误");
        }
        ThrowUtils.throwIf(spaceName == null, ErrorCode.PARAMS_ERROR, "名称不能为空");
        if (space.getSpaceName().length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");

        }


    }

    /**
     * 填充空间信息
     *
     * @param space 空间
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        //根据空间级别，填充空间信息
        SpaceLeveEnum spaceLeveEnum = SpaceLeveEnum.getSpaceValue(space.getSpaceLevel());
        if (spaceLeveEnum != null) {
            long maxSize = spaceLeveEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLeveEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }

    }

    /**
     * 获取空间查询条件
     *
     * @param spaceQueryRequest 空间查询条件
     * @return 查询条件
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        // 校验参数
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        queryWrapper.like(ObjUtil.isNotEmpty(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotNull(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        // 返回查询条件
        return queryWrapper;
    }

    /**
     * 获取单个空间封装类
     *
     * @param space   空间
     * @param request 请求
     * @return 空间封装类
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        //对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        //查询关联用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User loginUser = userService.getById(userId);
            UserVO userVO = userService.getUserVO(loginUser);
            spaceVO.setUserVO(userVO);
        }
        return spaceVO;
    }

    /**
     * 获取空间列表封装类
     *
     * @param spacePage 分页
     * @param request   请求
     * @return 空间列表封装类
     */
    @Override
    public Page<SpaceVO> getSpaceVOList(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }

        //对象列表转封装类列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .toList();

        //查询关联用户信息
        Set<Long> userIdSet = spaceVOList.stream()
                .map(SpaceVO::getUserId)
                .collect(Collectors.toSet());
        //批量查询用户
        Map<Long, List<UserVO>> userVOMap = userService.listByIds(userIdSet).stream()
                .map(userService::getUserVO)
                .collect(Collectors.groupingBy(UserVO::getId));
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            UserVO userVO = null;
            if (userVOMap.containsKey(userId)) {
                userVO = userVOMap.get(userId).get(0);
            }
            spaceVO.setUserVO(userVO);
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }


    // 1. 类级别定义线程安全的锁池（全局唯一）
    private static final ConcurrentHashMap<Long, Object> LOCK_MAP = new ConcurrentHashMap<>();

    /**
     * 创建空间
     *
     * @param spaceAddRequest 创建空间请求
     * @param loginUser       登录用户
     * @return 空间ID
     */
    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        //填充默认值
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest, space);
        if (spaceAddRequest.getSpaceName() == null) {
            space.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLeveEnum.COMMON.getValue());
        }
        //权限校验
        if (SpaceLeveEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        //校验
        this.validSpace(space, true);
        //填充默认值
        this.fillSpaceBySpaceLevel(space);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        //针对用户进行枷锁
        //String lock = String.valueOf(userId).intern();
        Object lock = LOCK_MAP.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            boolean exists = this.lambdaQuery()
                    .eq(Space::getUserId, space.getUserId())
                    .exists();
            if (exists) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间已存在");
            }
            //写入数据库
            boolean save = this.save(space);
            ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "创建空间失败");
            //返回新写入的数据id
            return space.getId();
        }
    }

}
