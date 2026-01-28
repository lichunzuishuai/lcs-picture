package com.lcs.lcspicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcs.lcspicture.model.dto.space.SpaceAddRequest;
import com.lcs.lcspicture.model.dto.space.SpaceQueryRequest;
import com.lcs.lcspicture.model.entity.Space;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author lcs
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2026-01-27 09:10:10
 */
public interface SpaceService extends IService<Space> {
    /**
     * 空间数据校验
     *
     * @param space 空间
     * @param add   是否为创建
     */
    void validSpace(Space space, Boolean add);

    /**
     * 根据空间级别填充空间信息
     *
     * @param space 空间
     */
    void fillSpaceBySpaceLevel(Space space);


    /**
     * 获取空间查询条件
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /*
     获取单个空间封装类
     */
    SpaceVO getSpaceVO(Space Space, HttpServletRequest request);

    /**
     * 获取空间列表封装类
     */
    Page<SpaceVO> getSpaceVOList(Page<Space> SpacePage, HttpServletRequest request);

    /**
     * 创建空间
     */
    Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);
}
