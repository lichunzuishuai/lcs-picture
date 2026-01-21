package com.lcs.lcspicture.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class PictureTagCategory {
    // 标签
    private List<String> tagList;
    // 分类
    private List<String> categoryList;
}
