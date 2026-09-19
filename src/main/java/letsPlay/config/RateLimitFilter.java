package letsPlay.config;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Simple in-memory sliding-window rate limiter (bonus feature).
 *
 * Applies a per-IP request budget over a 60 second window. Login/register
 * endpoints get a stricter budget to slow brute-force attempts.
 *
 * Kept intentionally simple: state lives in memory per server instance, which
 * is fine for a single-node deployment.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;

    private final Map<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();
    private final ErrorResponseWriter errorResponseWriter;
    private final boolean enabled;
    private final int generalLimit;
    private final int authLimit;

    public RateLimitFilter(ErrorResponseWriter errorResponseWriter, boolean enabled, int generalLimit, int authLimit) {
        this.errorResponseWriter = errorResponseWriter;
        this.enabled = enabled;
        this.generalLimit = generalLimit;
        this.authLimit = authLimit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled
                || request.getMethod().equals("OPTIONS")
                || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        int limit = path.startsWith("/api/auth/") ? authLimit : generalLimit;
        String key = clientIp(request) + "|" + (limit == authLimit ? "auth" : "api");

        long now = System.currentTimeMillis();
        Deque<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= limit) {
                errorResponseWriter.write(response, HttpStatus.TOO_MANY_REQUESTS,
                        "Too many requests. Please slow down and try again shortly.");
                return;
            }

            timestamps.offerLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}