package fingenie.com.fingenie.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID Filter
 * 
 * Ensures every request has a unique correlation ID for distributed tracing.
 * The ID is propagated via:
 * - MDC (for logging)
 * - Response header (for client debugging)
 * - Error responses (for support tickets)
 * 
 * If client provides X-Correlation-ID header, it will be used.
 * Otherwise, a new UUID is generated.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String REQUEST_PATH_MDC_KEY = "requestPath";
    public static final String REQUEST_METHOD_MDC_KEY = "requestMethod";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // Get or generate correlation ID
            String correlationId = extractOrGenerateCorrelationId(request);
            
            // Set up MDC context
            setupMDC(request, correlationId);
            
            // Add correlation ID to response header
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
        } finally {
            // Clear MDC to prevent memory leaks in thread pools
            clearMDC();
        }
    }

    /**
     * Extract correlation ID from request header or generate new one
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = generateCorrelationId();
        } else {
            // Sanitize external input (max 64 chars, alphanumeric + dash)
            correlationId = sanitizeCorrelationId(correlationId);
        }
        
        return correlationId;
    }

    /**
     * Generate a new unique correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Sanitize external correlation ID to prevent log injection
     */
    private String sanitizeCorrelationId(String id) {
        if (id == null) return generateCorrelationId();
        
        // Remove any characters that aren't alphanumeric or dash
        String sanitized = id.replaceAll("[^a-zA-Z0-9-]", "");
        
        // Limit length
        if (sanitized.length() > 64) {
            sanitized = sanitized.substring(0, 64);
        }
        
        return sanitized.isEmpty() ? generateCorrelationId() : sanitized;
    }

    /**
     * Set up MDC with request context
     */
    private void setupMDC(HttpServletRequest request, String correlationId) {
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        MDC.put(REQUEST_PATH_MDC_KEY, request.getRequestURI());
        MDC.put(REQUEST_METHOD_MDC_KEY, request.getMethod());
        
        // Add user ID if authenticated
        String userId = extractUserId();
        if (userId != null) {
            MDC.put(USER_ID_MDC_KEY, userId);
        }
    }

    /**
     * Extract user ID from security context
     */
    private String extractUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                // Principal might be username, custom user details, etc.
                Object principal = auth.getPrincipal();
                if (principal instanceof String) {
                    return (String) principal;
                } else {
                    return auth.getName();
                }
            }
        } catch (Exception e) {
            // Ignore - user ID is optional for logging
        }
        return null;
    }

    /**
     * Clear MDC to prevent context leaking between requests
     */
    private void clearMDC() {
        MDC.remove(CORRELATION_ID_MDC_KEY);
        MDC.remove(USER_ID_MDC_KEY);
        MDC.remove(REQUEST_PATH_MDC_KEY);
        MDC.remove(REQUEST_METHOD_MDC_KEY);
    }

    /**
     * Get current correlation ID from MDC
     * Utility method for use in other components
     */
    public static String getCurrentCorrelationId() {
        String id = MDC.get(CORRELATION_ID_MDC_KEY);
        return id != null ? id : "unknown";
    }

    /**
     * Get current user ID from MDC
     */
    public static String getCurrentUserId() {
        return MDC.get(USER_ID_MDC_KEY);
    }
}
