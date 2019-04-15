package com.cognibank.securityMicroservice.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "User nama or password was wrong")
public class UserNameOrPasswordWrongException extends RuntimeException {
}