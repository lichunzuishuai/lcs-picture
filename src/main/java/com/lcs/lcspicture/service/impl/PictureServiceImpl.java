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
import com.lcs.lcspicture.manager.CosManager;
import com.lcs.lcspicture.manager.upload.FilePictureUpload;
import com.lcs.lcspicture.manager.upload.PictureUploadTemplate;
import com.lcs.lcspicture.manager.upload.UrlPictureUpload;
import com.lcs.lcspicture.model.dto.file.UploadPictureResult;
import com.lcs.lcspicture.model.dto.picture.*;
import com.lcs.lcspicture.model.entity.Picture;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.enums.PictureReviewStatusEnum;
import com.lcs.lcspicture.model.vo.PictureVO;
import com.lcs.lcspicture.model.vo.UserVO;
import com.lcs.lcspicture.service.PictureService;
import com.lcs.lcspicture.mapper.PictureMapper;
import com.lcs.lcspicture.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    @Autowired
    private CosManager cosManager;

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
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片为空");
        }
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        // 判断是新增图片还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //判断图片是否存在
        if (pictureId != null) {
            Picture oldPictureId = getById(pictureId);
            ThrowUtils.throwIf(oldPictureId == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            if (!loginUser.getId().equals(oldPictureId.getUserId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限更新该图片");
            }
        }
        //上传图片,得到信息
        //按照用户id划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        //构造要入库的图片信息
        Picture picture = new Picture();
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
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //填充审核参数
        fillReviewParams(picture, loginUser);
        //如果picture不为空，表示更新，否则是更新
        Picture oldPicture = null;
        if (pictureId != null) {
            oldPicture = getById(pictureId);
            //如果是更新，需要补充id 和编辑时间
            //通过设置picture.setId(pictureId)，MyBatis-Plus能够识别这是更新而非插入操作因为如果要新增的话相同的id会报错
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        //如果是更新，需要删除对象存储中的图片
        if (pictureId != null && oldPicture != null) {
            this.clearPicture(oldPicture);
        }
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
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
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        if (StrUtil.isNotEmpty(searchText)) {
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
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
        ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "图片url过长");
        ThrowUtils.throwIf(introduction.length() > 880, ErrorCode.PARAMS_ERROR, "图片简介过长");
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
    public void clearPicture(Picture picture) {
        //判断该图片是否被多条记录使用
        String pictureUrl = picture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        //如果被多条记录使用，则不删除
        if (count > 1) {
            return;
        }
        //清理压缩图
        cosManager.deleteObject(pictureUrl);
        //清理缩略图
        String thumbnailUrl = picture.getThumbnailUrl();
        if (StrUtil.isNotEmpty(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
        //清理原图
        String originUrl = picture.getOriginUrl();
        if (StrUtil.isNotEmpty(originUrl)) {
            cosManager.deleteObject(originUrl);
        }

    }
}




