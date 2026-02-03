package com.lcs.lcspicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcs.lcspicture.common.DeleteRequest;
import com.lcs.lcspicture.model.dto.picture.*;
import com.lcs.lcspicture.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author lcs
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2026-01-18 22:48:32
 */
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     *
     * @param inputSource          图片输入源
     * @param pictureUploadRequest 上传图片请求参数
     * @param loginUser            登录用户
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 获取图片查询条件
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /*
     获取单个图片封装类
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片列表封装类
     */
    Page<PictureVO> getPictureVOList(Page<Picture> picturePage, HttpServletRequest request);

    /*
     图片数据校验
     */
    void validPicture(Picture picture);

    /*
    图片审合
     */
    void doReviewPicture(PictureReviewRequest pictureReviewRequest, User loginUser);

    /*
    填充图片审核参数
     */
    void fillReviewParams(Picture picture, User loginUser);

    /*
    批量上传图片
     */
    Integer uploadPictureByBatch(PictureUploadByBachRequest pictureUploadByBachRequest, User loginUser);

    /**
     * 清理图片文件
     *
     * @param oldPicture 旧图片
     */
    void clearPictureFile(Picture oldPicture);

    /*
        删除图片
         */
    void deletePicture(DeleteRequest deleteRequest, User loginUser);

    /*
    编辑图片
     */
    boolean editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 校验空间图片的权限
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 颜色搜图
     */
    List<PictureVO> colorSearch(Long spaceId, String piColor, User loginUser);

    /**
     * 批量编辑图片
     */
    void batchEditPicture(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);
}
