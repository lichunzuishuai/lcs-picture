package com.lcs.lcspicture.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片批量上传请求
 */
@Data
public class PictureUploadByBachRequest implements Serializable {

    /**
     * 搜索关键字
     */
    private String searchText;
    /**
     * 抓取数量
     */
    private Integer fetchCount = 10;

    /**
     * 图片前缀
     */
    private String prefixName;

    private static final long serialVersionUID = 1L;
}
