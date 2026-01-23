package com.lcs.lcspicture.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.lcs.lcspicture.config.CosClientConfig;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.manager.CosManager;
import com.lcs.lcspicture.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
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
        String uploadPath1 = processUploadPath(uploadPath);
        //3.获取本地文件
        File file = null;
        try {
            //创建临时文件
            file = File.createTempFile(uploadPath1, null);
            //处理文件来源
            processFile(inputSource, file);
            //4.上传到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath1, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //5.封装解析得到的图片信息
            return buildResult(originFileName, file, uploadPath1, imageInfo);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        } finally {
            //6.删除临时文件
            deleteTempFile(file);
        }


    }

    //为了解决批量抓取图片上传到对象存储时，如果上传的图片没有后缀，则默认添加.png后缀
    public String processUploadPath(String uploadPath) {
        // 处理路径方法
        final List<String> MUST_SUFFIX = Arrays.asList("jpeg", "jpg", "png", "webp", "gif", "bmp", "tiff", "svg");
        // 统一转为小写处理（保证后缀大小写不敏感）
        String lowerPath = uploadPath.toLowerCase();
        // 遍历所有合法后缀，检查是否以.后缀结尾
        for (String suffix : MUST_SUFFIX) {
            // 支持带参数链接（如image.png?width=200）
            if (lowerPath.matches(".*\\." + suffix + "($|\\?.*)")) {
                return uploadPath;
            }
        }
        return uploadPath + ".png"; // 默认补全为 PNG（兼容性最佳）
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
