package com.lcs.lcspicture.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcs.lcspicture.common.DeleteRequest;
import com.lcs.lcspicture.config.CosClientConfig;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.exception.ThrowUtils;
import com.lcs.lcspicture.manager.CosManager;
import com.lcs.lcspicture.manager.upload.FilePictureUpload;
import com.lcs.lcspicture.manager.upload.PictureUploadTemplate;
import com.lcs.lcspicture.manager.upload.UrlPictureUpload;
import com.lcs.lcspicture.model.dto.file.UploadPictureResult;
import com.lcs.lcspicture.model.dto.picture.*;
import com.lcs.lcspicture.model.entity.Picture;
import com.lcs.lcspicture.model.entity.Space;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.enums.PictureReviewStatusEnum;
import com.lcs.lcspicture.model.vo.PictureVO;
import com.lcs.lcspicture.model.vo.UserVO;
import com.lcs.lcspicture.service.PictureService;
import com.lcs.lcspicture.mapper.PictureMapper;
import com.lcs.lcspicture.service.SpaceService;
import com.lcs.lcspicture.service.UserService;
import com.lcs.lcspicture.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lcs
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2026-01-18 22:48:31
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {
    @Resource
    private UserService userService;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private CosManager cosManager;
    @Resource
    private SpaceService spaceService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传图片
     *
     * @param inputSource          上传的图片文件
     * @param pictureUploadRequest 上传图片请求参数
     * @param loginUser            登录用户
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        Long spaceId = pictureUploadRequest.getSpaceId();
        //校验空间是否存在
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            //校验是否空间权限
            if (!space.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无空间权限");
            }
            //校验空间额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容量不足");
            }
        }
        // 判断是新增图片还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null && pictureUploadRequest.getId() != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //判断图片是否存在
        if (pictureId != null) {
            Picture oldPictureId = getById(pictureId);
            ThrowUtils.throwIf(oldPictureId == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            if (!loginUser.getId().equals(oldPictureId.getUserId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限更新该图片");
            }
            //校验空间是否一致
            if (spaceId == null) {
                //没传spaceId
                if (oldPictureId.getSpaceId() != null) {
                    spaceId = oldPictureId.getSpaceId();
                }
            } else {
                //传了spaceId 必须与原始空间ID一致
                if (ObjUtil.notEqual(spaceId, oldPictureId.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }
        //上传图片,得到信息
        //按照用户id划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            //公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            //空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        //构造要入库的图片信息
        Picture picture = new Picture();
        //设置空间 id
        picture.setSpaceId(spaceId);
        //设置原图url
        picture.setOriginUrl(uploadPictureResult.getOriginUrl());
        //设置压缩图片url
        picture.setUrl(uploadPictureResult.getUrl());
        //设置缩略图
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        //支持外层传递图片名
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPrefixName())) {
            picName = pictureUploadRequest.getPrefixName();
        }
        picture.setName(picName);
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //填充审核参数
        fillReviewParams(picture, loginUser);
        //如果picture不为空，表示更新，否则是新增
        Picture oldPicture = null;
        if (pictureId != null) {
            oldPicture = getById(pictureId);
            //如果是更新，需要补充id 和编辑时间
            //通过设置picture.setId(pictureId)，MyBatis-Plus能够识别这是更新而非插入操作因为如果要新增的话相同的id会报错
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        //开启事务
        Long finalSpaceId = spaceId;
        Long finalPictureId = pictureId;
        Picture finalOldPicture = oldPicture;
        transactionTemplate.execute(transactionStatus -> {
            try {
                //操作数据库
                boolean result = this.saveOrUpdate(picture);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
                //更新空间额度
                if (finalSpaceId != null) {
                    if (finalPictureId == null) {
                        //新增
                        boolean update = spaceService.lambdaUpdate().eq(Space::getId, finalSpaceId).setSql("totalCount = totalCount + 1").setSql("totalSize = totalSize + " + picture.getPicSize()).update();
                        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
                    } else {
                        //更新
                        // 更新：容量 = 原容量 + (新大小 - 旧大小)，条数不变
                        long oldPicSize = finalOldPicture.getPicSize() == null ? 0L : finalOldPicture.getPicSize();
                        long newPicSize = picture.getPicSize();
                        long spaceSize = newPicSize - oldPicSize;
                        boolean update = spaceService.lambdaUpdate().eq(Space::getId, finalSpaceId).setSql("totalSize = totalSize + " + spaceSize).update();
                        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
                        this.clearPictureFile(finalOldPicture);
                    }
                }
            } catch (Exception e) {
                transactionStatus.setRollbackOnly(); // 手动回滚事务
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            }
            return true;
        });
        //如果是更新，需要删除对象存储中的图片
        /*if (pictureId != null && oldPicture != null) {
            this.clearPictureFile(oldPicture);
        }*/
        return PictureVO.objToVo(picture);
    }

    /**
     * 获取查询包装类
     *
     * @param pictureQueryRequest 查询条件
     * @return 查询包装类
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Boolean nullSpaceId = pictureQueryRequest.getNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        if (StrUtil.isNotEmpty(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText).or().like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.like(StrUtil.isNotEmpty(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.like(StrUtil.isNotEmpty(name), "name", name);
        queryWrapper.like(StrUtil.isNotEmpty(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotEmpty(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotEmpty(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        //>=startEditTime
        queryWrapper.ge(ObjUtil.isNotNull(startEditTime), "editTime", startEditTime);
        //<endEditTime
        queryWrapper.lt(ObjUtil.isNotNull(endEditTime), "editTime", endEditTime);
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.eq("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取单个图片封装类
     *
     * @param picture 图片
     * @param request 请求
     * @return 图片封装类
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        //对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        //查询关联用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User loginUser = userService.getById(userId);
            UserVO userVO = userService.getUserVO(loginUser);
            pictureVO.setUserVO(userVO);
        }
        return pictureVO;
    }

    /**
     * 获取图片列表封装类
     *
     * @param picturePage 分页
     * @param request     请求
     * @return 图片列表封装类
     */
    @Override
    public Page<PictureVO> getPictureVOList(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        //对象列表转封装类列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).toList();
        //获取用户id列表
        Set<Long> userIdList = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        //获取用户列表
        Map<Long, List<User>> collect = userService.listByIds(userIdList).stream().collect(Collectors.groupingBy(User::getId));
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (collect.containsKey(userId)) {
                user = collect.get(userId).get(0);
                pictureVO.setUserVO(userService.getUserVO(user));
            }
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 图片数据校验
     *
     * @param picture 图片
     */
    @Override
    public void validPicture(Picture picture) {
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR, "图片id不能为空");
        // 如果传递了 url，才校验
        if (StrUtil.isNotEmpty(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "图片url过长");
        }
        if (StrUtil.isNotEmpty(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 880, ErrorCode.PARAMS_ERROR, "图片简介过长");
        }

    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser            登录用户
     */
    @Override
    public void doReviewPicture(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.valuesOf(reviewStatus);
        //参数校验，如果状态为待审核，则不允许修改
        if (id == null || reviewStatus == null || PictureReviewStatusEnum.REVIEWING.equals(pictureReviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        //更新图片状态
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, picture);
        picture.setReviewerId(loginUser.getId());
        picture.setReviewTime(new Date());
        //操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片审核失败");
    }

    /**
     * 填充审核参数
     *
     * @param picture   图片
     * @param loginUser 登录用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        //管理员自动过审
        if (userService.isAdmin(loginUser)) {
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
        } else {
            //非管理员，待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 图片批量上传
     *
     * @param pictureUploadByBachRequest 图片上传批量请求
     * @param loginUser                  登录用户
     * @return 图片数量
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBachRequest pictureUploadByBachRequest, User loginUser) {
        //参数校验
        String searchText = pictureUploadByBachRequest.getSearchText();
        Integer fetchCount = pictureUploadByBachRequest.getFetchCount();
        ThrowUtils.throwIf(fetchCount > 20, ErrorCode.PARAMS_ERROR, "最多上传20张图片");
        String prefixName = pictureUploadByBachRequest.getPrefixName();
        if (StrUtil.isBlank(prefixName)) {
            prefixName = searchText;
        }
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document elements = null;
        try {
            //抓取内容
            elements = Jsoup.connect(fetchUrl).get();
        } catch (Exception e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "获取页面失败");
        }
        //解析内容
        Element dgControl = elements.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(dgControl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "获取元素失败");
        }
        Elements select = dgControl.select("img.mimg");
        Integer count = 0;
        for (Element element : select) {
            String fileUrl = element.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("获取元素失败{}", element);
                continue;
            }
            int questionMark = fileUrl.indexOf("?");
            if (questionMark != -1) {
                fileUrl = fileUrl.substring(0, questionMark);
            }
            //上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPrefixName(prefixName + (count + 1));
            try {
                PictureVO pictureVO = uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("上传图片成功{}", pictureVO);
                count++;
            } catch (Exception e) {
                log.error("上传图片失败{}", e);
                continue;
            }
            if (count >= fetchCount) {
                break;
            }
        }
        return count;

    }

    /**
     * 清理图片
     *
     * @param picture
     */
    @Async
    @Override
    public void clearPictureFile(Picture picture) {
        if (picture == null) {
            return;
        }

        String pictureUrl = picture.getUrl();
        String thumbnailUrl = picture.getThumbnailUrl();
        String originUrl = picture.getOriginUrl();
        String host = cosClientConfig.getHost();

        // 判断该图片URL是否被其他记录使用
        long count = this.lambdaQuery().eq(Picture::getUrl, pictureUrl).count();
        // 如果被多条记录使用，则不删除
        if (count > 1) {
            return;
        }

        // 清理压缩图
        if (StrUtil.isNotBlank(pictureUrl)) {
            String pictureUrlKey = extractKey(pictureUrl, host);
            if (StrUtil.isNotBlank(pictureUrlKey)) {
                cosManager.deleteObject(pictureUrlKey);
            }
        }
        // 清理缩略图
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            String thumbnailUrlKey = extractKey(thumbnailUrl, host);
            if (StrUtil.isNotBlank(thumbnailUrlKey)) {
                cosManager.deleteObject(thumbnailUrlKey);
            }
        }

        // 清理原图
        if (StrUtil.isNotBlank(originUrl)) {
            String originUrlKey = extractKey(originUrl, host);
            if (StrUtil.isNotBlank(originUrlKey)) {
                cosManager.deleteObject(originUrlKey);
            }
        }
    }

    /**
     * 从URL中提取对象Key
     */
    private String extractKey(String url, String host) {
        if (StrUtil.isBlank(url) || StrUtil.isBlank(host)) {
            return url;
        }
        if (url.startsWith(host)) {
            String key = url.substring(host.length());
            if (key.startsWith("/")) {
                key = key.substring(1);
            }
            return key;
        }
        return url;

    }

    /**
     * 删除图片
     *
     * @param deleteRequest 删除请求
     * @param loginUser     登录用户
     */
    @Override
    public void deletePicture(DeleteRequest deleteRequest, User loginUser) {
        Long deleteRequestId = deleteRequest.getId();
        // 判断图片是否存在
        Picture oldPicture = this.getById(deleteRequestId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅管理员或图片拥有者可删除
        this.checkPictureAuth(loginUser, oldPicture);
        //开启事务
        transactionTemplate.execute(transactionStatus -> {
            try {
                // 4.1 删除数据库中的图片记录
                boolean deleteResult = this.removeById(deleteRequestId);
                ThrowUtils.throwIf(!deleteResult, ErrorCode.OPERATION_ERROR, "图片删除失败，数据库操作异常");
                // 4.2 更新空间额度（仅当图片归属空间时）
                Long spaceId = oldPicture.getSpaceId();
                if (spaceId != null) {
                    // 空值兜底：防止picSize为null导致SQL拼接异常
                    long picSize = oldPicture.getPicSize() == null ? 0L : oldPicture.getPicSize();
                    boolean updateResult = spaceService.lambdaUpdate().eq(Space::getId, spaceId).setSql("totalCount = totalCount - 1").setSql("totalSize = totalSize - " + picSize).update();
                    ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "空间额度更新失败");
                }

                // 4.3 清除对象存储中的图片文件
                this.clearPictureFile(oldPicture);
                return true;
            } catch (Exception e) {
                // 异常时手动回滚事务，保证数据一致性
                transactionStatus.setRollbackOnly();
                // 抛出业务异常，让上层感知失败
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片删除失败，系统异常");
            }
        });
    }

    /**
     * 编辑图片
     *
     * @param pictureEditRequest 图片编辑请求
     * @param loginUser          登录用户
     */
    @Override
    public boolean editPicture(PictureEditRequest pictureEditRequest, User loginUser) {     //在此处讲实体类转和DTO进行转换
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureEditRequest, picture);
        //将tags转为json
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        //设置编辑时间
        picture.setEditTime(new Date());
        //校验图片
        this.validPicture(picture);
        //判断图片是否存在
        Long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //填充审核参数
        this.fillReviewParams(picture, loginUser);
        //判断图片权限
        this.checkPictureAuth(loginUser, oldPicture);
        //操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return result;
    }

    /**
     * 检查空间图片权限
     *
     * @param loginUser 登录用户
     * @param picture   图片
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            //私有空间
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (!loginUser.getId().equals(picture.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作");
            }
        } else {
            //公开图库
            if (!loginUser.getId().equals(picture.getUserId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作");
            }
        }

    }

    /**
     * 颜色搜索
     *
     * @param spaceId   空间id
     * @param piColor   颜色
     * @param loginUser 登录用户
     * @return 图片
     */
    @Override
    public List<PictureVO> colorSearch(Long spaceId, String piColor, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(piColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无空间权限");
        }
        //查询图片
        List<Picture> pictureList = this.lambdaQuery().eq(Picture::getSpaceId, spaceId).isNotNull(Picture::getPicColor).list();
        //如果为空返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        //将目标颜色转为Color对象
        Color decode = Color.decode(piColor);
        //计算相似度
        List<Picture> similarPictureList = pictureList.stream().sorted(Comparator.comparingDouble(picture -> {
                    //提取主色调
                    String hexColor = picture.getPicColor();
                    if (StrUtil.isEmpty(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color picColor = Color.decode(hexColor);
                    return -ColorSimilarUtils.calculateSimilarity(decode, picColor);
                }))
                .limit(12)
                .collect(Collectors.toList());

        return similarPictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest 图片批量编辑请求
     * @param loginUser                 登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchEditPicture(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        //参数校验
        ThrowUtils.throwIf(spaceId == null || CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //权限校验
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无空间权限");
        }
        //查询指定图片，进选择需要的字段
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (CollUtil.isEmpty(pictureList)) {
            return;
        }
        //批量更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotEmpty(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }

        });
        // 批量重命名
        String nameRole = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRole);

        //批量更新
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }


    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList nameRule
     * @param nameRule nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }

}




