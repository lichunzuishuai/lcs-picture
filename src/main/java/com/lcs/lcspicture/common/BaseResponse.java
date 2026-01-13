package com.lcs.lcspicture.common;

import com.lcs.lcspicture.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;
/*
 * 全局响应封装类
 */
@Data
public class BaseResponse<T> implements Serializable {
    /*
     返回码
     */
    private int code;
    /*
    返回数据
     */
    private T data;
    /*
    返回消息
     */
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
