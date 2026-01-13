package com.lcs.lcspicture.common;

import com.lcs.lcspicture.exception.ErrorCode;

/*
响应工具类
 */
public class ResultUtils {
    /**
     * 成功响应
     * @param data
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0,data,"ok");
    }
    /**
     * 失败响应
     * @param errorCode 错误码
     * @return 响应
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return  new BaseResponse<>(errorCode.getCode(),null,errorCode.getMessage());
    }
    /**
     * 失败响应
     * @param code 错误码
     * @param message 错误信息
     * @return 响应
     */
    public static<T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code,null,message);
    }
    /**
     * 失败响应
     * @param errorCode 错误码
     * @param message 错误信息
     * @return 响应
     */
    public  static<T> BaseResponse<T> error(ErrorCode errorCode , String message) {
        return new BaseResponse<>(errorCode.getCode(),null,message);
    }
}
