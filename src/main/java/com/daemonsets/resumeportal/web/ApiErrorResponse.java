package com.daemonsets.resumeportal.web;

import java.time.Instant;

public record ApiErrorResponse(
        String error,
        int status,
        String path,
        Instant timestamp
) {
    public static ApiErrorResponse of(String error, int status, String path) {
        return new ApiErrorResponse(error, status, path, Instant.now());
    }
}
