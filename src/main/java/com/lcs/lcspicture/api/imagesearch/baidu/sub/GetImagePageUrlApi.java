package com.lcs.lcspicture.api.imagesearch.baidu.sub;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GetImagePageUrlApi {
    public static String getImagePageUrl(String imageUrl) {
        // 构建表单参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");

        //获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 请求URL
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;
        String acsToken = "1769577505087_1769686413408_Y3GjJQBxx9TbLpKJWVUOho1uovAK7/vH6Ng2DcdiJO2c8NXBIcz9xPSogBezfKYPtB9uIAe9P3frj+TtULIJ3OngsPKZQWZWh9HytLSgmpXAsqh/PYJJC5DAOv2TCPv6AiFQi8n9auX4IotXMC2DmJYnclkOZ08VH0X3gIblsZ7I5o0gkqWWa+OpQDpNQKKTqbnAAdfs4GCpiuo7k+KAYITwCmVBtqib2ZLmGzU1DNvBxfJ0qc7Ly0WFqM/LbMWC205WrCCPLIKcntCQVkRkoxS/pYOfZup/GtdUwOuATHQRW5ZuUj+UJ5LtDEsvLaMdnR26LrcK1iHxSkTW7ASwzG3chqtLoQhb+uZqz9B5qbOVzHHl4TX9qW2+hprMi5g7akH4rUmNVj9TWDhTKbVrzynaQsR0SmdLWQut0lxTbOPDR82rJUWWOh8z5kM9OrqEnSsDIo+e8VqsK6KryasOPw==";
        try {
            // 发送请求
            HttpResponse response = HttpRequest.post(url)
                    .header("acs-token", acsToken)
                    // 表单数据
                    .form(formData)
                    // 执行请求
                    .execute();

            // ✅ 打印原始响应，方便调试
            log.info("响应状态码: {}", response.getStatus());
            log.info("响应内容: {}", response.body());

            if (response.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "百度图搜接口异常");
            }
            // 获取响应体
            String body = response.body();
            Map<String, Object> result = JSONUtil.toBean(body, Map.class);

            // ✅ 打印解析结果
            log.info("解析结果: {}", result);

            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "百度图搜接口异常");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = (String) data.get("url");
            if (rawUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "百度图搜接口异常");
            }
            return rawUrl;
        } catch (Exception e) {
            log.error("百度图搜接口异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "百度图搜接口异常");
        }
    }

    public static void main(String[] args) {
        String imageUrl = "https://lichunshen-1381464417.cos.ap-guangzhou.myqcloud.com/public/2014370424594481153/2026-01-23_QRpMmd4xf8sr2sFN.webp";
        String imagePageUrl = getImagePageUrl(imageUrl);
        System.out.println("搜索成功，结果url:" + imagePageUrl);
    }
}
