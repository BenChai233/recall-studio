package com.recallstudio.api;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ErrorResponse {
    private String code;
    private String message;
    private Map<String, Object> details;

    public ErrorResponse() {
    }

    public ErrorResponse(String code, String message, Map<String, Object> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }
}
