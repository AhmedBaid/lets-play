package letsPlay.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolationException;
import letsPlay.dto.ErrorResponseDTO;


@RestControllerAdvice
public class GlobalExceptionHandler {


    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, String message, WebRequest request) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                getPath(request),
                null);
        return new ResponseEntity<>(error, status);
    }


    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(GlobalException ex, WebRequest request) {
        return buildResponse(ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "The requested resource does not exist", request);
    }

    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoHandlerFound(
            org.springframework.web.servlet.NoHandlerFoundException ex, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "The requested resource does not exist", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(IllegalArgumentException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflict(IllegalStateException ex, WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "Operation could not be completed due to a conflicting state",
                request);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicateKey(DuplicateKeyException ex, WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT,
                "A record with the same unique value already exists (username or email)", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException ex,
            WebRequest request) {
        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                getPath(request),
                validationErrors);

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex,
            WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnreadableBody(HttpMessageNotReadableException ex,
            WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request body", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid value for path variable '" + ex.getName() + "'",
                request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingParameter(MissingServletRequestParameterException ex,
            WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Required parameter '" + ex.getParameterName() + "' is missing",
                request);
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleForbidden(AccessDeniedException ex, WebRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access Denied: you do not have permission to perform this action",
                request);
    }

    @ExceptionHandler({ JwtException.class, UsernameNotFoundException.class })
    public ResponseEntity<ErrorResponseDTO> handleUnauthorized(Exception ex, WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "You are not authorized to perform this action", request);
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(
            org.springframework.security.authentication.BadCredentialsException ex, WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password", request);
    }


    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotAllowed(Exception ex, WebRequest request) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not allowed for this resource", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpectedException(Exception ex, WebRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}