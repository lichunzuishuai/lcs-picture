package com.lcs.lcspicture.controller;

import com.lcs.lcspicture.annotation.AutoCheck;
import com.lcs.lcspicture.common.BaseResponse;
import com.lcs.lcspicture.common.ResultUtils;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.exception.ThrowUtils;
import com.lcs.lcspicture.model.dto.space.analyze.*;
import com.lcs.lcspicture.model.entity.Space;
import com.lcs.lcspicture.model.vo.space.analyze.*;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.service.SpaceAnalyzeService;
import com.lcs.lcspicture.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {
    @Resource
    private UserService userService;
    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    /**
     * 获取空间使用情况分析
     *
     * @param spaceAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(@RequestBody SpaceAnalyzeRequest spaceAnalyzeRequest,
                                                                        HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeResponse usageAnalyze = spaceAnalyzeService.getUsageAnalyze(spaceAnalyzeRequest, loginUser);
        return ResultUtils.success(usageAnalyze);
    }

    /**
     * 获取空间分类情况分析
     *
     * @param spaceCategoryAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
                                                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> categoryAnalyze = spaceAnalyzeService.getCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
        return ResultUtils.success(categoryAnalyze);
    }

    /**
     * 获取空间标签情况分析
     *
     * @param spaceTagAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                                          HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> tagAnalyze = spaceAnalyzeService.getTagsAnalyze(spaceTagAnalyzeRequest, loginUser);
        return ResultUtils.success(tagAnalyze);
    }

    /**
     * 获取空间大小情况分析
     *
     * @param spaceSizeAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
                                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeResponse> sizeAnalyze = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(sizeAnalyze);
    }

    /**
     * 获取用户上传空间分析
     *
     * @param spaceUserAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,
                                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceUserAnalyzeResponse> userSpaceAnalyze = spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, loginUser);
        return ResultUtils.success(userSpaceAnalyze);
    }

    /**
     * 获取空间排名情况分析
     *
     * @param spaceRankAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/rank")
    @AutoCheck(mustRole = "admin")
    public BaseResponse<List<Space>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        List<Space> spaceRankAnalyze = spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, user);
        return ResultUtils.success(spaceRankAnalyze);
    }
}
