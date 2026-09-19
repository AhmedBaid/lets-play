package letsPlay.exception;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import letsPlay.dto.ErrorResponseDTO;

/**
 * Formats errors that escape the application context (mainly exceptions thrown
 * in the security filter chain, e.g. {@link GlobalException}s from the JWT
 * filter, the authentication entry point and the access-denied handler).
 *
 * The servlet container re-dispatches such requests to /error, so they never
 * reach the DispatcherServlet's {@literal @RestControllerAdvice}. This
 * controller rebuilds the original {@link GlobalException} (status + message)
 * so the response keeps the exact same {@link ErrorResponseDTO} shape used
 * everywhere else in the API.
 */
@RestController
public class ApiErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorController.class);

    @RequestMapping("/error")
    public ResponseEntity<ErrorResponseDTO> handleError(HttpServletRequest request, HttpServletResponse response) {
        // If the response has already been committed there is nothing we can write.
        if (response.isCommitted()) {
            log.warn("Response already committed; cannot write error body for {}",
                    request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
            return null;
        }

        Throwable error = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An unexpected error occurred";

        if (error instanceof GlobalException globalException) {
            // Preserve the exact status + message of exceptions thrown outside
            // the DispatcherServlet (security filter chain).
            status = globalException.getStatus();
            message = globalException.getMessage();
        } else if (statusCode != null) {
            HttpStatus resolved = HttpStatus.resolve(statusCode);
            if (resolved != null) {
                status = resolved;
            }
            if (status.is4xxClientError()) {
                message = switch (status) {
                    case NOT_FOUND -> "The requested resource does not exist";
                    case METHOD_NOT_ALLOWED -> "HTTP method not allowed for this resource";
                    default -> status.getReasonPhrase();
                };
            }
        }

        if (error != null && !(error instanceof GlobalException)) {
            log.error("Error dispatched to /error while processing {}", request.getRequestURI(), error);
        }

        String path = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        ErrorResponseDTO body = new ErrorResponseDTO(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                null);

        return ResponseEntity.status(status).body(body);
    }
}