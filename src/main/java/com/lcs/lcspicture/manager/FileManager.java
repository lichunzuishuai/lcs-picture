package com.lcs.lcspicture.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.lcs.lcspicture.config.CosClientConfig;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.exception.ThrowUtils;
import com.lcs.lcspicture.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Deprecated //这是一个废弃掉的方法
@Slf4j
@Service
public class FileManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /*
    上传图片
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        //校验文件
        validPicture(multipartFile);
        //图片上传地址
        String UUID = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), UUID, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            //上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //计算图片的宽高比
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(uploadFileName));
            uploadPictureResult.setPicSize(multipartFile.getSize());
            uploadPictureResult.setPicWidth(width);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            // 返回可访问的地址
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("picture upload error", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        } finally {
            fileDelete(file);
        }
    }

    /*
     * 删除临时文件
     */
    private void fileDelete(File file) {
        if (file == null) {
            return;
        }
        //删除临时文件
        boolean delete = file.delete();
        if (!delete) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }

    /*
    文件校验
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        //校验文件大小
        long fileSize = multipartFile.getSize();
        final long MAX_SIZE = 2 * 1024 * 1024;
        ThrowUtils.throwIf(fileSize > MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
        //校验文件类型
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> allowTypes = Arrays.asList("jpeg", "png", "jpg", "webp");
        ThrowUtils.throwIf(!allowTypes.contains(suffix), ErrorCode.FORBIDDEN_ERROR, "文件类型不合法");
    }

    /*
    url传图片
    */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
        //校验文件
        validPicture(fileUrl);
        //图片上传地址
        String UUId = RandomUtil.randomString(16);
        String originFileName = FileUtil.mainName(fileUrl);
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), UUId,
                FileUtil.getSuffix(originFileName));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
            //下载图片
            HttpUtil.downloadFile(fileUrl, file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            //计算图片的宽高比
            double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(uploadFileName));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(width);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("picture upload error", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        }
    }

    /*
    url校验
    */
    private void validPicture(String fileUrl) {
        //1.校验非空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件url不能为空");
        //2..校验url格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //3.校验url协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"),
                ErrorCode.PARAMS_ERROR, "文件url格式错误");
        //4.发送HEAD请求验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (!response.isOk()) {
                return;
            }
            //5.文件存在，文件类型校验
            String contentType = response.header("Content-Type");
            final List<String> ALLOW_TYPES = Arrays.asList("image/jpeg", "image/png", "image/jpg", "image/webp");
            ThrowUtils.throwIf(!ALLOW_TYPES.contains(contentType.toLowerCase()), ErrorCode.PARAMS_ERROR, "文件类型不支持");
            //6.文件存在，文件大小校验
            String contentLength = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLength)) {
                try {
                    long parseLong = Long.parseLong(contentLength);
                    //允许最大文件大小
                    final long MAX_SIZE = 1024 * 1024;
                    ThrowUtils.throwIf(parseLong > 2 * MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小错误");
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }

    }

}
