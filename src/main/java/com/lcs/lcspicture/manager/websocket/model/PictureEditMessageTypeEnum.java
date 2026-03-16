package com.lcs.lcspicture.manager.websocket.model;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public enum PictureEditMessageTypeEnum {
    INFO("发送通知", "INFO"),
    ERROR("发送错误", "ERROR"),
    ENTER_EDIT("进入编辑状态", "ENTER_EDIT"),
    EXIT_EDIT("退出编辑状态", "EXIT_EDIT"),
    EDIT_ACTION("执行编辑操作", "EDIT_ACTION");

    private String text;
    private String value;

    PictureEditMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static PictureEditMessageTypeEnum getByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (PictureEditMessageTypeEnum pictureEditMessageTypeEnum : PictureEditMessageTypeEnum.values()) {
            if (pictureEditMessageTypeEnum.value.equals(value)) {
                return pictureEditMessageTypeEnum;
            }
        }
        return null;
    }
}
