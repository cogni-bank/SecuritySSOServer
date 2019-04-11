package com.cognibank.securityMicroservice.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserSessionExpiredException extends RuntimeException {
    public UserSessionExpiredException(String message) {
        super(message);
    }
}
