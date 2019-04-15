package com.cognibank.securityMicroservice.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "User management is down")
public class UserManagementServiceUnavailabeException extends RuntimeException {
}