package org.dragon.api.dto;

import lombok.Data;

/**
 * 统一 API 响应包装类
 *
 * @param <T> 响应数据类型
 * @author zhz
 * @version 1.0
 */
@Data
public class ApiResponse<T> {

    /** 响应码，0 表示成功 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /**
     * 成功响应
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(0);
        resp.setMessage("success");
        resp.setData(data);
        return resp;
    }

    /**
     * 成功响应（无数据）
     *
     * @return ApiResponse
     */
    public static ApiResponse<Void> success() {
        return success(null);
    }

    /**
     * 失败响应
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(code);
        resp.setMessage(message);
        return resp;
    }
}