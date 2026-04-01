package fingenie.com.fingenie.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import fingenie.com.fingenie.common.CustomException;
import fingenie.com.fingenie.dto.GoogleUserInfo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Service for Google OAuth2 token verification.
 * 
 * Verifies Google ID tokens and extracts user information.
 * Uses Google's official verification library for security.
 */
@Service
@Slf4j
public class GoogleOAuth2Service {

    @Value("${oauth2.google.client-id.web:}")
    private String webClientId;

    @Value("${oauth2.google.client-id.android:}")
    private String androidClientId;

    @Value("${oauth2.google.client-id.ios:}")
    private String iosClientId;

    private List<String> configuredClientIds = List.of();

    @PostConstruct
    public void init() {
        // Build list of valid client IDs (filter out empty ones)
        configuredClientIds = List.of(webClientId, androidClientId, iosClientId)
                .stream()
                .filter(id -> id != null && !id.isBlank())
                .toList();

        if (configuredClientIds.isEmpty()) {
            log.warn("No Google OAuth2 client IDs configured. Google Sign-In will be disabled.");
            return;
        }

        log.info("Google OAuth2 configured with {} client ID(s)", configuredClientIds.size());
    }

    /**
     * Verify Google ID token and extract user information.
     * 
     * @param idToken The ID token from Google Sign-In
     * @return GoogleUserInfo with verified user data
     * @throws CustomException if token is invalid or verification fails
     */
    public GoogleUserInfo verifyIdToken(String idToken) {
        return verifyIdToken(idToken, null);
    }

    public GoogleUserInfo verifyIdToken(String idToken, String platform) {
        if (configuredClientIds.isEmpty()) {
            throw new CustomException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GOOGLE_AUTH_DISABLED",
                    "Google Sign-In is not configured"
            );
        }

        if (idToken == null || idToken.isBlank()) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TOKEN",
                    "ID token is required"
            );
        }

        try {
            List<String> expectedAudiences = resolveExpectedAudiences(platform);
            GoogleIdTokenVerifier verifier = buildVerifier(expectedAudiences);
            GoogleIdToken token = verifier.verify(idToken);

            if (token == null) {
                log.warn("Invalid Google ID token received for platform {}", normalizePlatform(platform));
                throw new CustomException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_GOOGLE_TOKEN",
                        "Invalid or expired Google token"
                );
            }

            GoogleIdToken.Payload payload = token.getPayload();

            // Verify email is present and verified
            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "EMAIL_REQUIRED",
                        "Google account must have an email address"
                );
            }

            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "EMAIL_NOT_VERIFIED",
                        "Google email must be verified"
                );
            }

            // Extract user info
            return GoogleUserInfo.builder()
                    .id(payload.getSubject())
                    .email(email.toLowerCase())
                    .emailVerified(true)
                    .name((String) payload.get("name"))
                    .givenName((String) payload.get("given_name"))
                    .familyName((String) payload.get("family_name"))
                    .pictureUrl((String) payload.get("picture"))
                    .locale((String) payload.get("locale"))
                    .build();

        } catch (GeneralSecurityException e) {
            log.error("Security error verifying Google token", e);
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "TOKEN_VERIFICATION_ERROR",
                    "Error verifying Google token"
            );
        } catch (IOException e) {
            log.error("IO error verifying Google token", e);
            throw new CustomException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GOOGLE_SERVICE_ERROR",
                    "Unable to verify Google token. Please try again."
            );
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error verifying Google token", e);
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "UNEXPECTED_ERROR",
                    "An unexpected error occurred"
            );
        }
    }

    /**
     * Check if Google OAuth2 is properly configured.
     */
    public boolean isConfigured() {
        return !configuredClientIds.isEmpty();
    }

    private GoogleIdTokenVerifier buildVerifier(List<String> audiences) {
        return new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(audiences)
                .build();
    }

    private List<String> resolveExpectedAudiences(String platform) {
        String normalizedPlatform = normalizePlatform(platform);
        if (normalizedPlatform == null) {
            return configuredClientIds;
        }

        List<String> audiences = switch (normalizedPlatform) {
            case "web" -> filterClientIds(webClientId);
            case "android" ->
                    // Android native Google Sign-In typically issues an ID token for the web/server client ID.
                    filterClientIds(webClientId, androidClientId);
            case "ios" -> filterClientIds(webClientId, iosClientId);
            default -> configuredClientIds;
        };

        if (audiences.isEmpty()) {
            throw new CustomException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GOOGLE_AUTH_DISABLED",
                    "Google Sign-In is not configured for platform: " + normalizedPlatform
            );
        }

        return audiences;
    }

    private List<String> filterClientIds(String... clientIds) {
        List<String> audiences = new ArrayList<>();
        for (String clientId : clientIds) {
            if (clientId != null && !clientId.isBlank()) {
                audiences.add(clientId);
            }
        }
        return audiences;
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return null;
        }
        return platform.trim().toLowerCase(Locale.ROOT);
    }
}
