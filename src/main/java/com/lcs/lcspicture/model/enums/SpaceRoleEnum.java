package com.lcs.lcspicture.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间取权限枚举
 */
@Getter
public enum SpaceRoleEnum {
    VIEWER("查看者", "viewer"),
    EDITOR("编辑者", "editor"),
    ADMIN("管理员", "admin");

    private String text;

    private String value;

    SpaceRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据值获取空间角色枚举
     *
     * @param value 值
     * @return 空间角色枚举对象
     */
    public static SpaceRoleEnum getSpaceRoleValue(String value) {
        if (StrUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceRoleEnum spaceRoleEnum : SpaceRoleEnum.values()) {
            if (spaceRoleEnum.getValue().equals(value)) {
                return spaceRoleEnum;
            }
        }
        return null;
    }

    /**
     * 获取空间角色文本列表
     *
     * @return
     */
    public static List<String> getSpaceRoleTexts() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getText)
                .collect(Collectors.toList());
    }

    /**
     * 获取空间角色值列表
     *
     * @return
     */
    public static List<String> getSpaceRoleValues() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getValue)
                .collect(Collectors.toList());
    }
}
