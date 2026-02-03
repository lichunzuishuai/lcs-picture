package com.lcs.lcspicture.api.imagesearch.baidu.model;

import lombok.Data;

@Data
public class ImageSearchResult {

    /**
     * 缩略图
     */
    private String thumbUrl;

    /*
     * 来源地址
     */
    private String formUrl;
}
