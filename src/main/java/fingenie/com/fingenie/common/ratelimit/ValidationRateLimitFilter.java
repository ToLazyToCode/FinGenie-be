package fingenie.com.fingenie.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import fingenie.com.fingenie.common.error.ErrorResponse;
import fingenie.com.fingenie.common.filter.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/**
 * Rate Limiting Filter for Validation Endpoints
 * 
 * Protects validation endpoints from:
 * - Account enumeration attacks
 * - Brute force attempts
 * - API abuse / spam
 * 
 * Applied to paths matching /api/v1/validate/* and /api/v1/auth/validate/*
 * 
 * SECURITY NOTES:
 * - Uses client IP for rate limiting
 * - Considers X-Forwarded-For for proxied requests
 * - Returns 429 with Retry-After header
 * - Does NOT reveal rate limit specifics in response
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1) // Run early, but after CorrelationIdFilter
public class ValidationRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    // Paths that should be rate-limited for validation
    private static final Set<String> VALIDATION_PATH_PATTERNS = Set.of(
            "/api/v1/validate/",
            "/api/v1/auth/validate/",
            "/api/v1/users/validate/"
    );

    // Paths for auth rate limiting
    private static final Set<String> AUTH_PATH_PATTERNS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/signin"
    );

    // Paths for password reset rate limiting
    private static final Set<String> PASSWORD_RESET_PATTERNS = Set.of(
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/password/reset"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = extractClientIp(request);

        // Check which type of rate limiting applies
        RateLimitService.RateLimitType limitType = determineRateLimitType(path);

        if (limitType != null) {
            RateLimitService.RateLimitResult result = rateLimitService.checkLimit(clientIp, limitType);

            // Add rate limit headers
            addRateLimitHeaders(response, result, limitType, clientIp);

            if (!result.allowed()) {
                log.warn("Rate limit exceeded for {} on path {} from IP {}",
                        limitType, path, maskIp(clientIp));

                writeRateLimitResponse(response, result, request.getRequestURI());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determine which rate limit type applies to the request path
     */
    private RateLimitService.RateLimitType determineRateLimitType(String path) {
        if (path == null) return null;

        // Check validation endpoints
        for (String pattern : VALIDATION_PATH_PATTERNS) {
            if (path.startsWith(pattern) || path.contains("/validate")) {
                return RateLimitService.RateLimitType.VALIDATION;
            }
        }

        // Check auth/login endpoints
        for (String pattern : AUTH_PATH_PATTERNS) {
            if (path.equals(pattern) || path.startsWith(pattern)) {
                return RateLimitService.RateLimitType.LOGIN;
            }
        }

        // Check password reset endpoints
        for (String pattern : PASSWORD_RESET_PATTERNS) {
            if (path.equals(pattern) || path.startsWith(pattern)) {
                return RateLimitService.RateLimitType.PASSWORD_RESET;
            }
        }

        return null;
    }

    /**
     * Add standard rate limit headers to response
     */
    private void addRateLimitHeaders(
            HttpServletResponse response,
            RateLimitService.RateLimitResult result,
            RateLimitService.RateLimitType type,
            String clientIp
    ) {
        // Standard rate limit headers
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));

        if (!result.allowed()) {
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            response.setHeader("X-RateLimit-Reset", 
                    String.valueOf(Instant.now().plusSeconds(result.retryAfterSeconds()).getEpochSecond()));
        }
    }

    /**
     * Write rate limit exceeded response
     */
    private void writeRateLimitResponse(
            HttpServletResponse response,
            RateLimitService.RateLimitResult result,
            String path
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        if (correlationId == null) {
            correlationId = java.util.UUID.randomUUID().toString();
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .errorCode("RATE_LIMIT_EXCEEDED")
                .message("Too many validation attempts. Please try again later.")
                .path(path)
                .correlationId(correlationId)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Extract client IP considering proxies
     * 
     * SECURITY: In production, ensure X-Forwarded-For is only trusted from known proxies
     */
    private String extractClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For header (set by load balancers/proxies)
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // Take the first IP (original client)
            String[] ips = forwardedFor.split(",");
            return ips[0].trim();
        }

        // Check X-Real-IP header (nginx)
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp.trim();
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Mask IP for logging (privacy)
     */
    private String maskIp(String ip) {
        if (ip == null) return "unknown";
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".xxx";
        }
        return "xxx.xxx.xxx.xxx";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Only filter paths that need rate limiting
        return determineRateLimitType(path) == null;
    }
}
