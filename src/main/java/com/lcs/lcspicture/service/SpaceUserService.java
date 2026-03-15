package com.lcs.lcspicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lcs.lcspicture.model.dto.spaceuser.SpaceUserAddRequest;
import com.lcs.lcspicture.model.dto.spaceuser.SpaceUserQueryRequest;
import com.lcs.lcspicture.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcs.lcspicture.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author lcs
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2026-02-27 00:30:55
 */
public interface SpaceUserService extends IService<SpaceUser> {
    /**
     * 校验空间成员
     *
     * @param spaceUser 空间成员添加请求
     * @param add                 是否为创建
     */
    void validSpaceUser(SpaceUser spaceUser, Boolean add);

    /*
     获取单个空间成员包装类
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员列表包装类
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 创建空间成员
     */
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 获取空间成员查询条件
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);
}
