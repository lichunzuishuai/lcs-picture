package com.lcs.lcspicture.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlPictureUpload extends PictureUploadTemplate {

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        //下载文件到临时目录
        HttpUtil.downloadFile(fileUrl, file);
    }

    @Override
    protected String getOriginFileName(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.getName(fileUrl);
    }

    @Override
    protected void validPicture(Object inputSource) {
        //校验参数
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(StrUtil.isEmpty(fileUrl), ErrorCode.PARAMS_ERROR, "文件不能为空");
        //校验url格式
        ThrowUtils.throwIf(fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"),
                ErrorCode.PARAMS_ERROR, "文件格式错误");
        //校验URL协议
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //发送HEAD请求验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回，无需执行其他判断
            if (!response.isOk()) {
                return;
            }
            //文件存在 验证文件格式
            String contentType = response.header("Content-Type");
            //不为空，才校验是否合法，这样校验规则相对宽松
            if (StrUtil.isNotBlank(contentType)) {
                //允许上传的文件格式
                final List<String> ALLOW_TYPES = Arrays.asList("image/jpeg", "image/png", "image/jpg", "image/webp");
                ThrowUtils.throwIf(!ALLOW_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型不支持");
            }
            //文件存在验证文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long parseLong = Long.parseLong(contentLengthStr);
                    //允许最大文件大小
                    final long ONE_MB = 1024 * 1024;
                    ThrowUtils.throwIf(parseLong > 2 * ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小错误");
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        } finally {
            //关闭response释放资源
            if (response != null) {
                response.close();
            }

        }

    }
}
