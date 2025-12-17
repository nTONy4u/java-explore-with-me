package ru.practicum.explorewithme.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.explorewithme.dto.ApiError;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(NotFoundException e) {
        log.error("NotFoundException: {}", e.getMessage(), e);
        return buildErrorResponse(e, HttpStatus.NOT_FOUND, "The required object was not found.");
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflictException(ConflictException e) {
        log.error("ConflictException: {}", e.getMessage(), e);
        return buildErrorResponse(e, HttpStatus.CONFLICT, "For the requested operation the conditions are not met.");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiError> handleIllegalArgumentAndState(RuntimeException e) {
        log.error("Validation error: {}", e.getMessage(), e);
        return buildErrorResponse(e, HttpStatus.BAD_REQUEST, "Incorrectly made request.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException e) {
        log.error("Validation error: {}", e.getMessage(), e);

        List<String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> String.format("Field: %s. Error: %s. Value: %s",
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()))
                .collect(Collectors.toList());

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Incorrectly made request.")
                .message("Validation failed for one or more fields")
                .timestamp(LocalDateTime.now())
                .errors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException e) {
        log.error("Constraint violation: {}", e.getMessage(), e);

        List<String> errors = e.getConstraintViolations()
                .stream()
                .map(violation -> String.format("%s: %s",
                        violation.getPropertyPath(),
                        violation.getMessage()))
                .collect(Collectors.toList());

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Incorrectly made request.")
                .message("Constraint violation")
                .timestamp(LocalDateTime.now())
                .errors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.error("Type mismatch error: {}", e.getMessage(), e);

        String message = String.format("Failed to convert value of type '%s' to required type '%s'",
                e.getValue(), e.getRequiredType().getSimpleName());

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Incorrectly made request.")
                .message(message)
                .timestamp(LocalDateTime.now())
                .errors(Collections.singletonList(e.toString()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParams(MissingServletRequestParameterException e) {
        log.error("Missing parameter: {}", e.getMessage(), e);

        String message = String.format("Required parameter '%s' is not present", e.getParameterName());

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Incorrectly made request.")
                .message(message)
                .timestamp(LocalDateTime.now())
                .errors(Collections.singletonList(e.toString()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.error("Malformed JSON: {}", e.getMessage(), e);

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Incorrectly made request.")
                .message("Malformed JSON request")
                .timestamp(LocalDateTime.now())
                .errors(Collections.singletonList(e.getMostSpecificCause().getMessage()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllUncaughtException(Exception e) {
        log.error("Internal server error: {}", e.getMessage(), e);

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .reason("Internal server error.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .errors(Arrays.stream(e.getStackTrace())
                        .map(StackTraceElement::toString)
                        .limit(10)
                        .collect(Collectors.toList()))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

    private ResponseEntity<ApiError> buildErrorResponse(Exception e, HttpStatus status, String reason) {
        ApiError apiError = ApiError.builder()
                .status(status.name())
                .reason(reason)
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .errors(Collections.singletonList(e.toString()))
                .build();

        return ResponseEntity.status(status).body(apiError);
    }
}