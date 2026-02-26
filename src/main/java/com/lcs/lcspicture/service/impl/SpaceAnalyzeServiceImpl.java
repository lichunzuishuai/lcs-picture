package com.lcs.lcspicture.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.exception.ThrowUtils;
import com.lcs.lcspicture.mapper.SpaceMapper;
import com.lcs.lcspicture.model.dto.space.analyze.*;
import com.lcs.lcspicture.model.vo.space.analyze.*;
import com.lcs.lcspicture.model.entity.Picture;
import com.lcs.lcspicture.model.entity.Space;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.service.PictureService;
import com.lcs.lcspicture.service.SpaceAnalyzeService;
import com.lcs.lcspicture.service.SpaceService;
import com.lcs.lcspicture.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lcs
 * @createDate 2026-01-27 09:10:10
 */
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private PictureService pictureService;

    /**
     * 获取空间使用情况分析
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public SpaceUsageAnalyzeResponse getUsageAnalyze(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        //校验参数

        //全空间或者公共图库，需要从Picture表查询
        if (spaceAnalyzeRequest.isQueryPublic() || spaceAnalyzeRequest.isQueryAll()) {
            //权限校验仅管理员可以访问
            checkSpaceAnalyzeAuth(spaceAnalyzeRequest, loginUser);
            //统计公共图库的使用情况
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            //补充查询条件
            fillAnalyzeRequestQueryWrapper(spaceAnalyzeRequest, queryWrapper);
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long totalSize = pictureObjList.stream().mapToLong(obj -> obj instanceof Long ? (Long) obj : 0).sum();
            long usedCount = pictureObjList.size();
            //封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(totalSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            //公共图库无上限，无比例，使用情况
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {
            //指定空间，需要从Space表查询
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR, "空间ID不能为空");
            //空间存在性校验
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

            //校验权限：空间所有者或者管理员可以访问
            spaceService.checkSpaceAuth(loginUser, space);

            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            //计算空间使用比例
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            //计算空间使用比例
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    /**
     * 获取空间分类使用情况分析
     *
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
                                                                 User loginUser) {
        //校验参数
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        //权限校验
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        //查询条件构造
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        //补充查询条件
        fillAnalyzeRequestQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        queryWrapper.select("category as category", "COUNT(*) as count", "SUM(picSize) as totalSize")
                .groupBy("category");
        List<Map<String, Object>> maps = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return maps.stream().map(result -> {
            String category = result.get("category") != null ? result.get("category").toString() : "未分类";
            Long count = ((Number) (result.get("count"))).longValue();
            Long totalSize = ((Number) (result.get("totalSize"))).longValue();
            return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
        }).collect(Collectors.toList());
    }

    /**
     * 获取空间标签使用情况分析
     *
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getTagsAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        //权限校验
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        //补充查询条件
        fillAnalyzeRequestQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        queryWrapper.select("tags");
        //查询所有符合条件的标签
        List<String> tagsList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());
        //统计每个标签的使用次数
        Map<String, Long> stringLongMap = tagsList.stream().flatMap(tag -> JSONUtil.toList(tag, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        //按使用次数排序
        return stringLongMap.entrySet().stream().sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取空间大小使用情况分析
     *
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        //权限校验
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        //补充查询条件
        fillAnalyzeRequestQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
        queryWrapper.select("picSize");
        // 定义大小范围
        List<Long> sizeRange = Arrays.asList(100L * 1024, 500L * 1024, 1024L * 1024);
        Map<String, Long> collect = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number) size).longValue())
                .collect(Collectors.groupingBy(
                        size -> size < sizeRange.get(0) ? "<100KB" :
                                size < sizeRange.get(1) ? "100KB-500KB" :
                                        size < sizeRange.get(2) ? "500KB-1MB" : ">1MB",
                        Collectors.counting()));
        return collect.entrySet()
                .stream()
                .map(stringLongEntry -> new SpaceSizeAnalyzeResponse(stringLongEntry.getKey(), stringLongEntry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取空间用户使用情况分析
     *
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        //权限校验
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        //补充查询条件
        fillAnalyzeRequestQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        //分析维度：每日，每周，每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }
        queryWrapper.groupBy("period").orderByAsc("period");
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                }).collect(Collectors.toList());
    }

    /**
     * 获取空间排名使用情况分析
     *
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        //权限校验
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权访问");
        //构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN());
        //执行查询
        return spaceService.list(queryWrapper);
    }

    /**
     * 校验分析空间请求的参数
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        if (spaceAnalyzeRequest.isQueryPublic() || spaceAnalyzeRequest.isQueryAll()) {
            // 如果查询公共图库或所有图库，则只能由管理员访问
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权访问公共图库");
        } else {
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            // 如果查询的是指定空间，则需要判断空间是否存在且用户是否有权限访问
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    private void fillAnalyzeRequestQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        //所有空间
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll) {
            return;
        }
        //公共空间
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        if (queryPublic) {
            queryWrapper.isNull("spaceId");
            return;
        }
        //指定空间
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }
}

