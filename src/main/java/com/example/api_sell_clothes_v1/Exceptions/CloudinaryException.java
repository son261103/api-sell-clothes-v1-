package com.example.api_sell_clothes_v1.Exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CloudinaryException extends RuntimeException {
    private final HttpStatus status;

    public CloudinaryException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public CloudinaryException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}