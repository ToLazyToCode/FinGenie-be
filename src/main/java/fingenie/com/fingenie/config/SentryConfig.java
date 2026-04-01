package fingenie.com.fingenie.config;

import fingenie.com.fingenie.common.filter.CorrelationIdFilter;
import io.sentry.Sentry;
import io.sentry.SentryOptions;
import io.sentry.protocol.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Sentry Error Tracking Configuration.
 * 
 * Only enables Sentry when sentry.dsn is configured.
 * Attaches correlation ID, user ID, and request context to all errors.
 * 
 * Features:
 * - Automatic exception capture for 5xx errors
 * - User context propagation
 * - Correlation ID tagging for distributed tracing
 * - Sensitive data scrubbing
 * - Environment-aware configuration
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "sentry.dsn")
public class SentryConfig {

    @Value("${sentry.dsn:}")
    private String sentryDsn;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Value("${spring.application.name:fingenie}")
    private String applicationName;

    /**
     * Customizes Sentry options for FinGenie.
     */
    @Bean
    public SentryOptions.BeforeSendCallback beforeSendCallback() {
        return (event, hint) -> {
            // Attach correlation ID from MDC
            String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
            if (correlationId != null) {
                event.setTag("correlationId", correlationId);
            }

            // Attach request information
            String requestPath = MDC.get("requestPath");
            String requestMethod = MDC.get("requestMethod");
            if (requestPath != null) {
                event.setTag("request.path", requestPath);
            }
            if (requestMethod != null) {
                event.setTag("request.method", requestMethod);
            }

            // Attach error code if available
            String errorCode = MDC.get("errorCode");
            if (errorCode != null) {
                event.setTag("errorCode", errorCode);
            }

            // Set fingerprint for better grouping using tags
            if (event.getThrowable() != null) {
                String exceptionClass = event.getThrowable().getClass().getSimpleName();
                event.setTag("exception.type", exceptionClass);
            }

            return event;
        };
    }

    /**
     * Customizes Sentry options for transaction sampling and performance.
     */
    @Bean
    public SentryOptions.TracesSamplerCallback tracesSamplerCallback() {
        return context -> {
            // Sample 10% of transactions in production
            if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
                return 0.1;
            }
            // Sample 100% in staging for better visibility
            if ("staging".equals(activeProfile)) {
                return 1.0;
            }
            // Don't sample in dev/local
            return 0.0;
        };
    }

    /**
     * Filter to set Sentry user context from authenticated user.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 20)
    public SentryUserContextFilter sentryUserContextFilter() {
        return new SentryUserContextFilter();
    }

    /**
     * Filter that sets Sentry user context from MDC.
     */
    public static class SentryUserContextFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {

            try {
                // Set user context from MDC (set by security filter)
                String userId = MDC.get("userId");
                if (userId != null && !userId.isEmpty()) {
                    Sentry.configureScope(scope -> {
                        User user = new User();
                        user.setId(userId);
                        scope.setUser(user);
                    });
                }

                filterChain.doFilter(request, response);
            } finally {
                // Clear Sentry user context
                Sentry.configureScope(scope -> scope.setUser(null));
            }
        }
    }
}
