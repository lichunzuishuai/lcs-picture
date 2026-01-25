package com.lcs.lcspicture.manager;

import cn.hutool.core.io.FileUtil;
import com.lcs.lcspicture.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  你要把文件保存到对象存储的那个位置
     * @param file 你要上传的文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 获取对象
     *
     * @param key 你要获取的对象存储中的那个位置
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        // 创建PutObject请求
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 创建图片处理请求
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);

        // 图片处理规则列表
        List<PicOperations.Rule> picOperationsRules = new ArrayList<>();

        //图片压缩
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule rule = new PicOperations.Rule();
        rule.setBucket(cosClientConfig.getBucket());
        rule.setFileId(webpKey);
        rule.setRule("imageMogr2/format/webp");
        picOperationsRules.add(rule);

        //图片缩略图
        //仅对图片大小超过 50KB 的图片生效
        if (file.length() > 50 * 1024) {
            PicOperations.Rule thumbnail = new PicOperations.Rule();
            //拼接缩略图的路径
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnail.setFileId(thumbnailKey);
            thumbnail.setBucket(cosClientConfig.getBucket());
            thumbnail.setRule(String.format("imageMogr2/thumbnail/%sx%s", 128, 128));//等比缩放
            picOperationsRules.add(thumbnail);
        }


        // 添加图片处理规则
        picOperations.setRules(picOperationsRules);
        // 设置图片处理参数
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象存储中的文件
     *
     * @param key 你要获取的对象存储中的那个位置
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
