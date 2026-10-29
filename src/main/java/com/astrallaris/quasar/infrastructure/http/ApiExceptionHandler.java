package com.astrallaris.quasar.infrastructure.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.astrallaris.quasar.application.exceptions.BucketAlreadyExistsException;
import com.astrallaris.quasar.application.exceptions.InvalidNameException;
import com.astrallaris.quasar.application.exceptions.NotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(BucketAlreadyExistsException.class)
    public ProblemDetail handleBucketAlreadyExists(BucketAlreadyExistsException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidNameException.class)
    public ProblemDetail handleInvalidName(InvalidNameException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
