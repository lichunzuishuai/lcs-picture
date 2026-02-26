package com.lcs.lcspicture.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcs.lcspicture.model.dto.space.analyze.*;
import com.lcs.lcspicture.model.vo.space.analyze.*;
import com.lcs.lcspicture.model.entity.Space;
import com.lcs.lcspicture.model.entity.User;

import java.util.List;

/**
 * @author lcs
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2026-01-27 09:10:10
 */
public interface SpaceAnalyzeService extends IService<Space> {
    /**
     * 获取空间使用情况分析
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     * @return
     */
    SpaceUsageAnalyzeResponse getUsageAnalyze(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);

    /**
     * 获取空间分类情况分析
     *
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceCategoryAnalyzeResponse> getCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 获取空间标签情况分析
     *
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceTagAnalyzeResponse> getTagsAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 获取空间大小情况分析
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 获取空间用户上传情况分析
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 获取空间排名情况分析
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
