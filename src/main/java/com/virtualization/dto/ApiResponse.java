package com.virtualization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private Instant timestamp;
    private int count;

    public static <T> ApiResponse<T> success(T data, String message, int count) {
        return new ApiResponse<>(true, data, message, Instant.now(), count);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return success(data, message, 1);
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Success");
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, Instant.now(), 0);
    }
}
