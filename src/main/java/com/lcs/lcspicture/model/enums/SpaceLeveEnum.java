package com.lcs.lcspicture.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 空间级别枚举
 */
@Getter
public enum SpaceLeveEnum {
    COMMON("普通版", 0, 100, 100L * 1024 * 1024),
    PROFESSIONAL("专业版", 1, 1000, 1000L * 1024 * 1024),
    FLAGSHIP("旗舰版", 2, 10000L, 10000L * 1024 * 1024);

    /**
     * 空间级别名称
     */
    private final String text;
    /**
     * 空间级别值
     */
    private final int value;
    /**
     * 空间最大图片数量
     */
    private final long maxCount;
    /**
     * 最空间最大存储大小
     */
    private final long maxSize;

    SpaceLeveEnum(String text, int value, long maxCount, long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    public static SpaceLeveEnum getSpaceValue(Integer value) {
        // 判断value是否为空
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceLeveEnum spaceLeveEnum : SpaceLeveEnum.values()) {
            if (spaceLeveEnum.value == value) {
                return spaceLeveEnum;
            }
        }
        return null;
    }
}
