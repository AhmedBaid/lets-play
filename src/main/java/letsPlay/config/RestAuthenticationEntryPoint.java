package letsPlay.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import letsPlay.exception.GlobalException;

/**
 * Returns a 401 when an unauthenticated request hits a protected endpoint.
 * The thrown exception is formatted as JSON by the /error controller.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        throw new GlobalException("Authentication required: please provide a valid token",
                HttpStatus.UNAUTHORIZED);
    }
}