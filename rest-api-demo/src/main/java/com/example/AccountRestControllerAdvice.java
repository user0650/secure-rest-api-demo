package com.example;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by sunxiaopeng on 2016/11/16.
 */

@ControllerAdvice
public class AccountRestControllerAdvice { // 1

    @ResponseBody
    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    IllegalArgumentException illegalArgumentExceptionHandler(Exception e) { // 2
        return new IllegalArgumentException(e.getMessage()); // 3
    }

}
