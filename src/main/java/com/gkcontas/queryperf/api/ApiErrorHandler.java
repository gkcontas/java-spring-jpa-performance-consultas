package com.gkcontas.queryperf.api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns a rejected request body into a response that names what was wrong.
 *
 * <p>Without this the framework answers 400 with an empty body, and the explicit
 * constraint messages the project insists on never reach the caller.
 */
@RestControllerAdvice
public class ApiErrorHandler {

    public record ValidationError(String message, List<FieldProblem> fields) {}

    public record FieldProblem(String field, String message) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> onInvalidBody(MethodArgumentNotValidException thrown) {
        List<FieldProblem> fields = thrown.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldProblem(error.getField(), message(error)))
                .sorted(java.util.Comparator.comparing(FieldProblem::field))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ValidationError("the request body was rejected", fields));
    }

    private static String message(FieldError error) {
        return error.getDefaultMessage() == null ? "is invalid" : error.getDefaultMessage();
    }
}
