package com.lcs.lcspicture.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.lcs.lcspicture.config.CosClientConfig;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.manager.CosManager;
import com.lcs.lcspicture.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosManager cosManager;
    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 定义模版方法
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadFilePrefix) {
        //1.文件校验
        validPicture(inputSource);
        //2.获取上传地址
        String uuid = RandomUtil.randomString(16);
        String originFileName = getOriginFileName(inputSource);
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),
                uuid, FileUtil.getSuffix(originFileName));
        String uploadPath = String.format("%s/%s", uploadFilePrefix, uploadFileName);
        //3.获取本地文件
        File file = null;
        try {
            //创建临时文件
            file = File.createTempFile(uploadPath, null);
            //处理文件来源
            processFile(inputSource, file);
            //4.上传到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //获取到图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            //获得处理转换后的所有图片信息
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                //获取压缩之后的文件信息
                CIObject ciObject = objectList.get(0);
                //缩略图默认就等于压缩图
                CIObject thumbnail = ciObject;
                if(objectList.size() > 1 && ObjUtil.isNotEmpty(objectList.get(1))){
                     thumbnail = objectList.get(1);
                }
                //5.封装解析得到的图片信息
                return buildResult(uploadPath, originFileName, ciObject,thumbnail);
            }
            //5.封装解析得到的图片信息
            return buildResult(originFileName, file, uploadPath, imageInfo);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        } finally {
            //6.删除临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 构建上传结果
     *
     * @param originFileName 原始文件名
     * @param ciObject       压缩后的文件信息
     * @return
     */
    private UploadPictureResult buildResult(String uploadPath, String originFileName, CIObject ciObject, CIObject thumbnail) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        Integer width = ciObject.getWidth();
        Integer height = ciObject.getHeight();
        double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + ciObject.getKey());
        uploadPictureResult.setOriginUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(originFileName);
        uploadPictureResult.setPicSize(ciObject.getSize().longValue());
        uploadPictureResult.setPicWidth(ciObject.getWidth());
        uploadPictureResult.setPicHeight(ciObject.getHeight());
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(ciObject.getFormat());
        //缩略图
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnail.getKey());
        return uploadPictureResult;
    }

    /**
     * 处理输入源并生成本地文件
     *
     * @param inputSource 输入源
     * @param file        文件
     * @throws IOException 输入源处理异常
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 获取原始文件名
     *
     * @param inputSource 输入源
     * @return 原始文件名
     */
    protected abstract String getOriginFileName(Object inputSource);

    /**
     * 校验输入源（本地文件或Url）
     *
     * @param inputSource 输入源
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 构建上传结果
     *
     * @param originFileName 原始文件名
     * @param file           文件
     * @param uploadPath     上传路径
     * @param imageInfo      图片信息
     * @return 上传结果
     */
    private UploadPictureResult buildResult(String originFileName, File file, String uploadPath, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(originFileName);
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(imageInfo.getWidth());
        uploadPictureResult.setPicHeight(imageInfo.getHeight());
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     *
     * @param file 文件
     */
    private void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean del = FileUtil.del(file);
        if (!del) {
            log.error("file del error={}", file.getAbsolutePath());
        }
    }


}
