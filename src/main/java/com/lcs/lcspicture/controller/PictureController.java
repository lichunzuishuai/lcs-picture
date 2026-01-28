package com.lcs.lcspicture.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lcs.lcspicture.annotation.AutoCheck;
import com.lcs.lcspicture.common.BaseResponse;
import com.lcs.lcspicture.common.DeleteRequest;
import com.lcs.lcspicture.common.ResultUtils;
import com.lcs.lcspicture.constant.UserConstant;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.exception.ThrowUtils;
import com.lcs.lcspicture.model.dto.picture.*;
import com.lcs.lcspicture.model.entity.Picture;
import com.lcs.lcspicture.model.entity.Space;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.enums.PictureReviewStatusEnum;
import com.lcs.lcspicture.model.vo.PictureTagCategory;
import com.lcs.lcspicture.model.vo.PictureVO;
import com.lcs.lcspicture.service.PictureService;
import com.lcs.lcspicture.service.SpaceService;
import com.lcs.lcspicture.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SpaceService spaceService;

    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            // 初始容量
            .initialCapacity(1024)
            // 最大存储数量
            .maximumSize(10_000)
            // 设置缓存失效时间5分钟
            .expireAfterWrite(5L, TimeUnit.MINUTES).build();

    /**
     * 文件上传图片
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    // @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * Url上传图片
     *
     * @param pictureUploadRequest 上传图片参数
     * @param request              请求
     */
    @PostMapping("/upload/url")
    // @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /*
    图片批量上传
     */
    @PostMapping("/upload/batch")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> uploadPictureByBatch(@RequestBody PictureUploadByBachRequest pictureUploadByBachRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBachRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long count = pictureService.uploadPictureByBatch(pictureUploadByBachRequest, loginUser);
        return ResultUtils.success(count);
    }

    /**
     * 根据id删除图片
     *
     * @param deleteRequest id
     * @return 删除结果
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.deletePicture(deleteRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片
     *
     * @param pictureUpdateRequest 修改图片参数
     * @param request              请求
     * @return
     */
    @PostMapping("/update")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        //校验参数
        ThrowUtils.throwIf(pictureUpdateRequest == null || pictureUpdateRequest.getId() < 0, ErrorCode.PARAMS_ERROR);
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        //将tags转为json
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        //校验图片
        pictureService.validPicture(picture);
        //判断图片是否存在
        Long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //填充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        //操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    /*
     * 获取图片列表(仅管理员可用)
     */
    @PostMapping("/list/page")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> getPicturePageList(@RequestBody PictureQueryRequest pictureQueryRequest) {
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(page);
    }

    /*
     * 获取图片列表(封装类)
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> getPicturePageListVO(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        //校验空间权限
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            //公共图库
            //普通用户默认显示审核通过的图片
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            //私有空间
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作");
            }
        }
        //查询数据库
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryRequest));
        //获取封装类
        return ResultUtils.success(pictureService.getPictureVOList(page, request));
    }

    /*
     * 获取图片列表(封装类)
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        //普通用户默认显示审核通过的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //构建缓存key
        String questionCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(questionCondition.getBytes());
        String cacheKey = String.format("lcs:listPictureVOByPage:%s", hashKey);
        //先查询本地缓存
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            //本地缓存命中则返回
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(pictureVOPage);
        }
        //本地缓存未命中则查询Redis分布式缓存
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        cachedValue = operations.get(cacheKey);
        if (cachedValue != null) {
            //分布式缓存命中把值先写入本地缓存然后返回
            LOCAL_CACHE.put(cacheKey, cachedValue);
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(pictureVOPage);
        }
        //本地缓存和分布式缓存未命中查询数据库
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryRequest));
        //获取封装类
        Page<PictureVO> pictureVOList = pictureService.getPictureVOList(page, request);
        String cacheValue = JSONUtil.toJsonStr(pictureVOList);
        //写入本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        //构建RedisKey过期时间5-10分钟，防止缓存雪崩
        int expireTime = 300 + RandomUtil.randomInt(0, 300);
        //写入Redis
        operations.set(cacheKey, cacheValue, expireTime, TimeUnit.SECONDS);
        //返回结果
        return ResultUtils.success(pictureVOList);
    }

    /*
    根据id获取图片(管理员)
     */
    @GetMapping("/get")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(@RequestParam Long id) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        //判断图片是否存在
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picture);
    }

    /*
    根据id获取图片(用户)
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(@RequestParam long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        //判断图片是否存在
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        //空间权限校验
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            User loginUser = userService.getLoginUser(request);
            pictureService.checkPictureAuth(loginUser, picture);
        }
        //获取审核状态
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.valuesOf(picture.getReviewStatus());
        //审核通过的图片所有人都可以查看
        if (PictureReviewStatusEnum.PASS.equals(pictureReviewStatusEnum)) {
            return ResultUtils.success(pictureService.getPictureVO(picture, request));
        }
        // 审核未通过的情况下，只有图片拥有者可以查看
        User loginUser = userService.getLoginUser(request);
        if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "图片未通过审核，无权查看");
        }
        // 图片拥有者或管理员可以查看未通过审核的图片
        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }

    /*
    修改图片（用户)
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        //校验参数
        ThrowUtils.throwIf(pictureEditRequest == null || pictureEditRequest.getId() < 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 获取图片标签和分类
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /*
     * 图片审核
     */
    @PostMapping("/review")
    @AutoCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doReviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null || pictureReviewRequest.getId() < 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doReviewPicture(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }


}
