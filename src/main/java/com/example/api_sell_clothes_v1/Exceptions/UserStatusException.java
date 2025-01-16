package com.example.api_sell_clothes_v1.Exceptions;

import org.springframework.security.core.AuthenticationException;

public class UserStatusException extends AuthenticationException {
    public UserStatusException(String message) {
        super(message);
    }
}