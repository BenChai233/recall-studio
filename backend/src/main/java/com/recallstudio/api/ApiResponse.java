package com.recallstudio.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;

    public ApiResponse() {}

    public ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "ok", data);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
