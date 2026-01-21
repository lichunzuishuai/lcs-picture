package com.lcs.lcspicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcs.lcspicture.model.dto.file.UploadPictureResult;
import com.lcs.lcspicture.model.dto.picture.PictureQueryRequest;
import com.lcs.lcspicture.model.dto.picture.PictureUploadRequest;
import com.lcs.lcspicture.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

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
     * @param multipartFile        上传的图片文件
     * @param pictureUploadRequest 上传图片请求参数
     * @param loginUser            登录用户
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

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
}
