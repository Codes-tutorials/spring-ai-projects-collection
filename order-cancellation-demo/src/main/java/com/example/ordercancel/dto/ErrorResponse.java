package com.example.ordercancel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "Standard error response envelope")
public class ErrorResponse {

    @Schema(description = "ISO-8601 timestamp of the error", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP error reason", example = "Bad Request")
    private String error;

    @Schema(description = "Human-readable error message")
    private String message;

    @Schema(description = "The request path that triggered the error", example = "/api/orders")
    private String path;
}
